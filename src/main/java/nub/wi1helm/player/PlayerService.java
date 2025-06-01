// Updated PlayerService.java with Simple Error Logging
package nub.wi1helm.player;

import com.google.gson.*;
import nub.wi1helm.server.ServerProfile;
import nub.wi1helm.server.ServerTeam;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static nub.wi1helm.Main.logger;

public class PlayerService {

    // Singleton instance
    private static volatile PlayerService instance;
    private static final Object lock = new Object();

    private static final String BASE_URL = "http://player-service:8081";

    private final HttpClient httpClient;
    private final Gson gson;

    // Private constructor to prevent direct instantiation
    private PlayerService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        this.gson = new GsonBuilder()
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
    }

    // Thread-safe singleton getter
    public static PlayerService getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new PlayerService();
                }
            }
        }
        return instance;
    }

    public CompletableFuture<ServerProfile> loadPlayerProfile(@NotNull String uuid, @NotNull String username) {
        return getPlayerProfile(uuid, username)
                .thenCompose(profileWithStatus -> {
                    if (profileWithStatus.profile != null) {
                        logger.info("PlayerService: Loaded existing player data for {}.", username);
                        return CompletableFuture.completedFuture(profileWithStatus.profile);
                    }

                    logger.info("PlayerService: Player profile not found for {}. Attempting to create.", username);
                    return createPlayerProfile(uuid)
                            .thenCompose(createdProfile -> {
                                if (createdProfile != null) {
                                    logger.info("PlayerService: Player profile newly created for {}.", username);
                                    createdProfile.setFirstJoin(true);
                                    return CompletableFuture.completedFuture(createdProfile);
                                } else {
                                    logger.warn("PlayerService: Failed to create profile for {}. Returning null.", username);
                                    return CompletableFuture.completedFuture(null);
                                }
                            })
                            .exceptionallyCompose(ex -> {
                                if (ex.getCause() != null && ex.getCause().getMessage() != null && ex.getCause().getMessage().contains("409 Conflict")) {
                                    logger.warn("PlayerService: Race condition detected for {}. Profile already exists, retrying GET.", username);
                                    return getPlayerProfile(uuid, username)
                                            .thenApply(retryProfileWithStatus -> {
                                                if (retryProfileWithStatus.profile != null) {
                                                    logger.info("PlayerService: Successfully retrieved profile for {} after conflict.", username);
                                                    return retryProfileWithStatus.profile;
                                                } else {
                                                    logger.error("PlayerService: Failed to retrieve profile for {} even after conflict resolution.", username);
                                                    return null;
                                                }
                                            });
                                }
                                logger.error("PlayerService: Unexpected error during profile creation for {}: {}", username, ex.getMessage());
                                return CompletableFuture.completedFuture(null);
                            });
                })
                .exceptionally(ex -> {
                    logger.error("PlayerService: Failed to load/create player profile for {}: {}", username, ex.getMessage());
                    return null; // Return null instead of throwing
                });
    }

    private static class ProfileStatus {
        ServerProfile profile;
        int statusCode;

        ProfileStatus(ServerProfile profile, int statusCode) {
            this.profile = profile;
            this.statusCode = statusCode;
        }
    }

    private CompletableFuture<ProfileStatus> getPlayerProfile(@NotNull String uuid, @NotNull String username) {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/profiles/" + uuid))
                .GET()
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(8))
                .build();

        return httpClient.sendAsync(getRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    logger.debug("PlayerService (GET): Received HTTP response for {}. Status: {}, Body: {}", username, response.statusCode(), response.body());

                    if (response.statusCode() == 404) {
                        return new ProfileStatus(null, response.statusCode());
                    }

                    if (response.statusCode() != 200) {
                        logger.error("PlayerService (GET): Unexpected response status {} for {}: {}", response.statusCode(), username, response.body());
                        return new ProfileStatus(null, response.statusCode());
                    }

                    PlayerApiResponse apiResponse = parseApiResponse(response.body(), username);
                    if (apiResponse == null) {
                        return new ProfileStatus(null, response.statusCode());
                    }

                    ServerProfile profile = createServerProfile(apiResponse);
                    if (profile != null) {
                        profile.setFirstJoin(false);
                    }
                    return new ProfileStatus(profile, response.statusCode());
                })
                .exceptionally(ex -> {
                    logger.error("PlayerService (GET): HTTP request failed for {}: {}", username, ex.getMessage());
                    return new ProfileStatus(null, 0);
                });
    }

    private CompletableFuture<ServerProfile> createPlayerProfile(@NotNull String uuid) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("uuid", uuid);

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/profiles"))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(8))
                .build();

        return httpClient.sendAsync(postRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    logger.debug("PlayerService (POST): Received HTTP response for {}. Status: {}, Body: {}", uuid, response.statusCode(), response.body());

                    if (response.statusCode() == 409) {
                        logger.warn("PlayerService (POST): Profile already exists for {}", uuid);
                        return null; // Will trigger race condition handling in parent method
                    }
                    if (response.statusCode() != 201) {
                        logger.error("PlayerService (POST): Unexpected response status {} for {}: {}", response.statusCode(), uuid, response.body());
                        return null;
                    }

                    PlayerApiResponse apiResponse = parseApiResponse(response.body(), uuid);
                    if (apiResponse == null) {
                        return null;
                    }

                    return createServerProfile(apiResponse);
                })
                .exceptionally(ex -> {
                    logger.error("PlayerService (POST): HTTP request failed for {}: {}", uuid, ex.getMessage());
                    return null;
                });
    }

    private PlayerApiResponse parseApiResponse(String jsonBody, String identifier) {
        try {
            return gson.fromJson(jsonBody, PlayerApiResponse.class);
        } catch (JsonSyntaxException e) {
            logger.error("PlayerService: Failed to parse JSON response for {}: {}", identifier, e.getMessage());
            return null;
        }
    }

    private ServerProfile createServerProfile(PlayerApiResponse apiResponse) {
        try {
            return new ServerProfile(
                    apiResponse.getUuid(),
                    apiResponse.getUsername(),
                    apiResponse.getTotalPlaytimeTicks(),
                    apiResponse.getDeltaPlaytimeTicks(),
                    apiResponse.isBanned(),
                    apiResponse.getBanExpiresAt(),
                    ServerTeam.fromString(apiResponse.getTeam()),
                    apiResponse.getLastLoginAt(),
                    apiResponse.getCreatedAt()
            );
        } catch (Exception e) {
            logger.error("PlayerService: Failed to create ServerProfile: {}", e.getMessage());
            return null;
        }
    }

    // Method to gracefully shutdown the HttpClient when needed
    public void shutdown() {
        logger.info("PlayerService: Shutdown requested");
    }
}