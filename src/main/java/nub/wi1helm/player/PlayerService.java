// nub.wi1helm.player/PlayerService.java
package nub.wi1helm.player;

import com.google.gson.*;
import nub.wi1helm.server.ServerProfile;
import nub.wi1helm.server.ServerTeam;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException; // Import for wrapping exceptions

import static nub.wi1helm.Main.logger;

public class PlayerService {

    private static final String BASE_URL = "http://localhost:8081"; // Corrected port if 8081 is your Go service
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(java.time.Duration.ofSeconds(5))
            .build();

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (src, typeOfSrc, context) -> {
                return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.toString());
            })
            .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, typeOfT, context) -> {
                if (json.isJsonNull() || json.getAsString().isEmpty()) {
                    return null;
                }
                return Instant.parse(json.getAsString());
            })
            .create();

    /**
     * Attempts to load a player's profile. If the profile does not exist, it creates one.
     * Handles race conditions during creation.
     *
     * @param uuid The player's UUID.
     * @param username The player's username (used for logging/error messages).
     * @return A CompletableFuture that will complete with the ServerProfile, or exceptionally if an error occurs.
     */
    public CompletableFuture<ServerProfile> loadPlayerProfile(@NotNull String uuid, @NotNull String username) {
        return getPlayerProfile(uuid, username)
                .thenCompose(profileWithStatus -> {
                    // If profile was found directly, return it.
                    if (profileWithStatus.profile != null) {
                        logger.info("PlayerService: Loaded existing player data for {}.", username);
                        return CompletableFuture.completedFuture(profileWithStatus.profile);
                    }

                    // Profile not found, attempt to create it
                    logger.info("PlayerService: Player profile not found for {}. Attempting to create.", username);
                    return createPlayerProfile(uuid)
                            .thenCompose(createdProfile -> {
                                // Creation successful
                                logger.info("PlayerService: Player profile newly created for {}.", username);
                                createdProfile.setFirstJoin(true);
                                return CompletableFuture.completedFuture(createdProfile);
                            })
                            .exceptionallyCompose(ex -> {
                                // If creation failed due to conflict (race condition), retry GET
                                if (ex.getCause() instanceof RuntimeException && ex.getCause().getMessage().contains("409 Conflict")) {
                                    logger.warn("PlayerService: Race condition detected for {}. Profile already exists, retrying GET.", username);
                                    return getPlayerProfile(uuid, username)
                                            .thenApply(retryProfileWithStatus -> {
                                                if (retryProfileWithStatus.profile != null) {
                                                    logger.info("PlayerService: Successfully retrieved profile for {} after conflict.", username);
                                                    return retryProfileWithStatus.profile;
                                                } else {
                                                    throw new RuntimeException("Failed to retrieve profile for " + username + " even after conflict resolution.");
                                                }
                                            });
                                }
                                // Propagate other creation errors
                                throw new CompletionException(ex);
                            });
                })
                .exceptionally(ex -> {
                    logger.error("PlayerService: Exception during player profile loading/creation for {}: {}", username, ex.getMessage(), ex);
                    Throwable actualCause = ex.getCause() != null ? ex.getCause() : ex;
                    throw new RuntimeException("Failed to load/create your player data: " + actualCause.getMessage());
                });
    }

    // Private helper to wrap the profile and status for internal use
    private static class ProfileStatus {
        ServerProfile profile;
        int statusCode; // Keep status code for specific handling

        ProfileStatus(ServerProfile profile, int statusCode) {
            this.profile = profile;
            this.statusCode = statusCode;
        }
    }

    // Private helper to perform the GET request and initial parsing
    private CompletableFuture<ProfileStatus> getPlayerProfile(@NotNull String uuid, @NotNull String username) {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/profiles/" + uuid)) // Corrected endpoint to /profiles
                .GET()
                .header("Accept", "application/json")
                .timeout(java.time.Duration.ofSeconds(8))
                .build();

        return HTTP_CLIENT.sendAsync(getRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    logger.debug("PlayerService (GET): Received HTTP response for {}. Status: {}, Body: {}", username, response.statusCode(), response.body());

                    if (response.statusCode() == 404) {
                        return new ProfileStatus(null, response.statusCode()); // Profile not found
                    }

                    if (response.statusCode() != 200) {
                        throw new RuntimeException(String.format("Unexpected response status from data service (GET %d): %s", response.statusCode(), response.body()));
                    }

                    PlayerApiResponse apiResponse = parseApiResponse(response.body(), username);
                    ServerProfile profile = createServerProfile(apiResponse);
                    profile.setFirstJoin(false); // Definitely not first join if retrieved successfully
                    return new ProfileStatus(profile, response.statusCode());
                });
    }

    // Private helper to perform the POST request for creation
    private CompletableFuture<ServerProfile> createPlayerProfile(@NotNull String uuid) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("uuid", uuid); // Ensure UUID is in the request body

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/profiles")) // Corrected endpoint to /profiles
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(java.time.Duration.ofSeconds(8))
                .build();

        return HTTP_CLIENT.sendAsync(postRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    logger.debug("PlayerService (POST): Received HTTP response for {}. Status: {}, Body: {}", uuid, response.statusCode(), response.body());

                    if (response.statusCode() == 409) { // Conflict - already exists
                        throw new RuntimeException("409 Conflict: Player profile already exists for " + uuid);
                    }
                    if (response.statusCode() != 201) { // Expect 201 Created
                        throw new RuntimeException(String.format("Unexpected response status from data service (POST %d): %s", response.statusCode(), response.body()));
                    }

                    PlayerApiResponse apiResponse = parseApiResponse(response.body(), uuid);
                    return createServerProfile(apiResponse);
                });
    }

    // Centralized JSON parsing logic
    private PlayerApiResponse parseApiResponse(String jsonBody, String identifier) {
        try {
            return GSON.fromJson(jsonBody, PlayerApiResponse.class);
        } catch (JsonSyntaxException e) {
            logger.error("PlayerService: Failed to parse JSON response for {}: {}", identifier, jsonBody, e);
            throw new RuntimeException("Failed to parse player data: " + e.getMessage());
        }
    }

    // Centralized ServerProfile creation logic
    private ServerProfile createServerProfile(PlayerApiResponse apiResponse) {
        return new ServerProfile(
                apiResponse.getUuid(),
                apiResponse.getUsername(),
                apiResponse.getTotalPlaytimeTicks(), // Use getTotalPlaytimeTicks directly
                apiResponse.getDeltaPlaytimeTicks(), // Use getDeltaPlaytimeTicks directly
                apiResponse.isBanned(),
                apiResponse.getBanExpiresAt(),
                ServerTeam.fromString(apiResponse.getTeam()),
                apiResponse.getLastLoginAt(),
                apiResponse.getCreatedAt()
        );
    }
}