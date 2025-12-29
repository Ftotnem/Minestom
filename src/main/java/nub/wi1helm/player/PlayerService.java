package nub.wi1helm.player;

import com.google.gson.*;
import nub.wi1helm.server.ServerProfile;
import nub.wi1helm.server.ServerTeam;
import org.jetbrains.annotations.NotNull;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static nub.wi1helm.Main.logger;

public class PlayerService {
    private static volatile PlayerService instance;
    private static final Object lock = new Object();
    private static final String BASE_URL = "http://localhost:8081"; //"http://player-service:8081";

    private final HttpClient httpClient;
    private final Gson gson;

    private PlayerService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (src, typeOfSrc, context) ->
                        src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.toString()))
                .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, typeOfT, context) -> {
                    if (json.isJsonNull() || json.getAsString().isEmpty()) return null;
                    return Instant.parse(json.getAsString());
                })
                .create();
    }

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

    /**
     * Loads player profile with essential data only (team, banned status, first join)
     * Playtime data is fetched separately from GameService
     */
    public CompletableFuture<ServerProfile> loadPlayerProfile(@NotNull String uuid, @NotNull String username) {
        return getPlayerProfile(uuid, username)
                .thenCompose(profile -> {
                    if (profile != null) {
                        logger.info("PlayerService: Loaded existing player {} (team: {}, banned: {}, currentPlaytime: {})",
                                username, profile.getServerTeam(), profile.isBanned(), profile.getCurrentPlaytime());
                        return CompletableFuture.completedFuture(profile);
                    }

                    logger.info("PlayerService: Creating new profile for {}", username);
                    return createPlayerProfile(uuid, username);
                })
                .exceptionally(ex -> {
                    handleLoadingError(ex, username);
                    return null;
                });
    }

    private CompletableFuture<ServerProfile> getPlayerProfile(@NotNull String uuid, @NotNull String username) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/profiles/" + uuid))
                .GET()
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(8))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 404) {
                        return null; // Profile doesn't exist
                    }
                    if (response.statusCode() != 200) {
                        logger.error("Unexpected response {} for {}: {}",
                                response.statusCode(), username, response.body());
                        return null;
                    }

                    return parseProfileResponse(response.body(), username, false);
                })
                .exceptionally(ex -> {
                    logConnectionError(ex, "GET", username);
                    return null;
                });
    }

    private CompletableFuture<ServerProfile> createPlayerProfile(@NotNull String uuid, @NotNull String username) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("uuid", uuid);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/profiles"))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(8))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 409) {
                        // Race condition - profile was created by another thread
                        logger.warn("Race condition detected for {}, retrying GET", username);
                        return null; // This will trigger a retry in the calling method
                    }
                    if (response.statusCode() != 201) {
                        logger.error("Failed to create profile for {}: {} - {}",
                                username, response.statusCode(), response.body());
                        return null;
                    }

                    return parseProfileResponse(response.body(), username, true);
                })
                .exceptionally(ex -> {
                    logConnectionError(ex, "POST", username);
                    return null;
                })
                .thenCompose(profile -> {
                    if (profile == null) {
                        // Retry GET in case of race condition
                        logger.info("Retrying profile fetch for {} after creation conflict", username);
                        return getPlayerProfile(uuid, username);
                    }
                    return CompletableFuture.completedFuture(profile);
                });
    }

    private ServerProfile parseProfileResponse(String jsonBody, String username, boolean isFirstJoin) {
        try {
            // --- IMPORTANT: ADD/CONFIRM THESE LINES TO LOG RECEIVED JSON AND PARSED OBJECT ---
            logger.info("DEBUG: PlayerService: Raw JSON response for {}: {}", username, jsonBody); // Log the raw JSON received
            // --- END IMPORTANT ADDED LINES ---

            PlayerApiResponse apiResponse = gson.fromJson(jsonBody, PlayerApiResponse.class);
            if (apiResponse == null) {
                logger.error("Null API response for {}", username);
                return null;
            }

            // --- IMPORTANT: LOG THE PARSED VALUES FROM THE PlayerApiResponse ---
            logger.info("DEBUG: PlayerService: Parsed PlayerApiResponse for {}: uuid={}, username={}, team={}, currentPlaytime={}, deltaPlaytime={}",
                    username,
                    apiResponse.getUuid(),
                    apiResponse.getUsername(),
                    apiResponse.getTeam(),
                    apiResponse.getCurrentPlaytime(),     // Verify this is correct after deserialization
                    apiResponse.getDeltaPlaytime());
            // --- END IMPORTANT ADDED LINES ---

            ServerProfile profile = new ServerProfile(
                    apiResponse.getUuid(),
                    apiResponse.getUsername(),
                    apiResponse.getTeamUsername(),
                    apiResponse.getCurrentPlaytime(),    // This should now be using the value from apiResponse
                    apiResponse.getDeltaPlaytime(),
                    apiResponse.isBanned(),
                    apiResponse.getBanExpiresAt(),
                    ServerTeam.fromString(apiResponse.getTeam()),
                    apiResponse.getLastLoginAt(),
                    apiResponse.getCreatedAt()
            );

            profile.setFirstJoin(isFirstJoin);
            return profile;

        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse profile JSON for {}: {}", username, e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Failed to create ServerProfile for {}: {}", username, e.getMessage());
            return null;
        }
    }

    private void handleLoadingError(Throwable ex, String username) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        if (cause instanceof ConnectException) {
            logger.error("FATAL: Cannot connect to player service for {}. Check service at {}",
                    username, BASE_URL);
        } else {
            logger.error("Failed to load/create profile for {}: {}", username, cause.getMessage());
        }
    }

    private void logConnectionError(Throwable ex, String method, String username) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        if (cause instanceof ConnectException) {
            logger.error("Connection refused during {} for {}. Check player service at {}",
                    method, username, BASE_URL);
        } else {
            logger.error("HTTP {} request failed for {}: {}", method, username, cause.getMessage());
        }
    }

    public void shutdown() {
        logger.info("PlayerService: Shutdown requested");
    }
}