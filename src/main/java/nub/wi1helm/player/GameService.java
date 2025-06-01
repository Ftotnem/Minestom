package nub.wi1helm.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static nub.wi1helm.Main.logger; // Assuming this imports your logger

public class GameService {

    private static volatile GameService instance;
    private static final Object lock = new Object();

    // Base URL for your Go Game Service
    private static final String GAME_SERVICE_BASE_URL = "http://game-service:8082";

    private final HttpClient httpClient;
    private final Gson gson;

    private GameService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.gson = new GsonBuilder().create();
    }

    public static GameService getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new GameService();
                }
            }
        }
        return instance;
    }

    // --- Response DTOs matching Go service API ---

    // Matches Go's api.PlaytimeResponse
    private static class PlaytimeResponse {
        private double playtime; // Field name from Go: `json:"playtime"`
        public double getPlaytime() { return playtime; }
    }

    // Matches Go's api.DeltaPlaytimeResponse
    private static class DeltaPlaytimeResponse {
        private double deltatime; // Field name from Go: `json:"deltatime"`
        public double getDeltatime() { return deltatime; }
    }

    // Matches Go's api.TeamTotalPlaytimeResponse
    private static class TeamTotalPlaytimeResponse {
        private String teamId;        // Field name from Go: `json:"teamId"`
        private double totalPlaytime; // Field name from Go: `json:"totalPlaytime"`
        public String getTeamId() { return teamId; }
        public double getTotalPlaytime() { return totalPlaytime; }
    }

    // Matches Go's api.PlayerOnlineStatusResponse
    private static class PlayerOnlineStatusResponse {
        private String uuid;     // Field name from Go: `json:"uuid"`
        private boolean isOnline; // Field name from Go: `json:"isOnline"`
        public String getUuid() { return uuid; }
        public boolean isOnline() { return isOnline; }
    }

    // --- API Methods Reflecting Go Endpoints ---

    /**
     * Fetches a player's total accumulated playtime from the Game Service.
     * Corresponds to Go endpoint: `GET /game/player/{uuid}/playtime`
     *
     * @param uuid The UUID of the player.
     * @return A CompletableFuture that completes with the player's total playtime, or 0.0 if not found or an error occurs.
     */
    public CompletableFuture<Double> getPlayerTotalPlaytime(String uuid) {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(GAME_SERVICE_BASE_URL + "/game/player/" + uuid + "/playtime")) // Updated URI
                .GET()
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(8))
                .build();

        return httpClient.sendAsync(getRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 404) {
                        logger.info("GameService: Total playtime not found for {}. Returning 0.0.", uuid);
                        return 0.0;
                    }
                    if (response.statusCode() != 200) {
                        logger.error("GameService: Unexpected response status for player total playtime (GET {}): {}", response.statusCode(), response.body());
                        throw new RuntimeException(String.format("Unexpected response status from GameService (GET playtime %d): %s", response.statusCode(), response.body()));
                    }

                    try {
                        PlaytimeResponse apiResponse = gson.fromJson(response.body(), PlaytimeResponse.class);
                        // Safely get the playtime, handle potential null if JSON structure is off
                        return apiResponse != null ? apiResponse.getPlaytime() : 0.0;
                    } catch (JsonSyntaxException e) {
                        logger.error("GameService: Failed to parse player total playtime JSON response for {}: {}", uuid, response.body(), e);
                        throw new RuntimeException("Failed to parse player total playtime data: " + e.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    logger.error("GameService: Exception fetching player total playtime for {}: {}", uuid, ex.getMessage(), ex);
                    return 0.0; // Return 0.0 on exception
                });
    }

    /**
     * Fetches a player's delta playtime from the Game Service.
     * Corresponds to Go endpoint: `GET /game/player/{uuid}/deltatime`
     *
     * @param uuid The UUID of the player.
     * @return A CompletableFuture that completes with the player's delta playtime, or 0.0 if not found or an error occurs.
     */
    public CompletableFuture<Double> getPlayerDeltaPlaytime(String uuid) {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(GAME_SERVICE_BASE_URL + "/game/player/" + uuid + "/deltatime")) // Updated URI
                .GET()
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(8))
                .build();

        return httpClient.sendAsync(getRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 404) {
                        logger.info("GameService: Delta playtime not found for {}. Returning 0.0.", uuid);
                        return 0.0;
                    }
                    if (response.statusCode() != 200) {
                        logger.error("GameService: Unexpected response status for player delta playtime (GET {}): {}", response.statusCode(), response.body());
                        throw new RuntimeException(String.format("Unexpected response status from GameService (GET deltatime %d): %s", response.statusCode(), response.body()));
                    }

                    try {
                        DeltaPlaytimeResponse apiResponse = gson.fromJson(response.body(), DeltaPlaytimeResponse.class);
                        // Safely get the deltatime, handle potential null if JSON structure is off
                        return apiResponse != null ? apiResponse.getDeltatime() : 0.0;
                    } catch (JsonSyntaxException e) {
                        logger.error("GameService: Failed to parse player delta playtime JSON response for {}: {}", uuid, response.body(), e);
                        throw new RuntimeException("Failed to parse player delta playtime data: " + e.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    logger.error("GameService: Exception fetching player delta playtime for {}: {}", uuid, ex.getMessage(), ex);
                    return 0.0; // Return 0.0 on exception
                });
    }

    /**
     * Fetches the total playtime for a specific team from the Game Service.
     * Corresponds to Go endpoint: `GET /game/team/{teamId}/playtime`
     *
     * @param teamId The ID of the team.
     * @return A CompletableFuture that will complete with the team's total playtime, or 0.0 if not found or an error occurs.
     */
    public CompletableFuture<Double> getTeamPlaytime(String teamId) {
        // Correct endpoint: Now includes teamId as a path variable
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(GAME_SERVICE_BASE_URL + "/game/team/" + teamId + "/playtime")) // Updated URI
                .GET()
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(8))
                .build();

        return httpClient.sendAsync(getRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 404) {
                        logger.info("GameService: Team playtime not found for team {}. Returning 0.0.", teamId);
                        return 0.0;
                    }
                    if (response.statusCode() != 200) {
                        logger.error("GameService: Unexpected response status for team playtime (GET {}): {}", response.statusCode(), response.body());
                        throw new RuntimeException(String.format("Unexpected response status from GameService (GET team playtime %d): %s", response.statusCode(), response.body()));
                    }

                    try {
                        // Parse into the new TeamTotalPlaytimeResponse object which now includes teamId
                        TeamTotalPlaytimeResponse apiResponse = gson.fromJson(response.body(), TeamTotalPlaytimeResponse.class);
                        // Safely get the playtime, handle potential null if JSON structure is off
                        return apiResponse != null ? apiResponse.getTotalPlaytime() : 0.0;
                    } catch (JsonSyntaxException e) {
                        logger.error("GameService: Failed to parse team playtime JSON response for team {}: {}", teamId, response.body(), e);
                        throw new RuntimeException("Failed to parse team playtime data: " + e.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    logger.error("GameService: Exception fetching team playtime for team {}: {}", teamId, ex.getMessage(), ex);
                    return 0.0;
                });
    }



    // Add methods for HandlePlayerOnline, HandlePlayerOffline, HandleRefreshOnline, HandleBanPlayer, HandleUnbanPlayer
    // if your Java client needs to trigger these actions. They would typically be POST requests.


    // DTO for PlayerUUIDRequest
    private static class PlayerUUIDRequest {
        private String uuid;
        public PlayerUUIDRequest(String uuid) { this.uuid = uuid; }
    }

    // Method to gracefully shutdown the HttpClient when needed
    public void shutdown() {
        // In newer JDKs, HttpClient might have a close method for more explicit resource release.
        // For simple cases, letting it be garbage collected after application shutdown is often sufficient.
        logger.info("GameService: Shutdown requested");
    }
}