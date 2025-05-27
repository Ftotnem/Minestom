// nub.wi1helm.player/GameService.java
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

import static nub.wi1helm.Main.logger; // Assuming Main.logger is accessible

public class GameService {

    private static final String GAME_SERVICE_BASE_URL = "http://localhost:8082";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final Gson GSON = new GsonBuilder().create();

    private static class PlaytimeResponse {
        private double playtime;
        public double getPlaytime() { return playtime; }
    }

    private static class DeltaPlaytimeResponse {
        private double deltatime;
        public double getDeltatime() { return deltatime; }
    }

    public CompletableFuture<Double> getPlayerTotalPlaytime(String uuid) {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(GAME_SERVICE_BASE_URL + "/playtime/" + uuid))
                .GET()
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(8))
                .build();

        return HTTP_CLIENT.sendAsync(getRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    //logger.debug("GameService: Received HTTP response for total playtime of {}. Status: {}, Body: {}", uuid, response.statusCode(), response.body());

                    if (response.statusCode() == 404) {
                        logger.info("GameService: Total playtime not found for {}. Returning 0.0.", uuid);
                        return 0.0;
                    }
                    if (response.statusCode() != 200) {
                        logger.error("GameService: Unexpected response status for total playtime (GET {}): {}", response.statusCode(), response.body());
                        throw new RuntimeException(String.format("Unexpected response status from GameService (GET playtime %d): %s", response.statusCode(), response.body()));
                    }

                    try {
                        PlaytimeResponse apiResponse = GSON.fromJson(response.body(), PlaytimeResponse.class);
                        return apiResponse.getPlaytime();
                    } catch (JsonSyntaxException e) {
                        logger.error("GameService: Failed to parse total playtime JSON response for {}: {}", uuid, response.body(), e);
                        throw new RuntimeException("Failed to parse total playtime data: " + e.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    logger.error("GameService: Exception fetching total playtime for {}: {}", uuid, ex.getMessage(), ex);
                    return 0.0;
                });
    }

    /**
     * Fetches a player's delta playtime from the Go GameService.
     * Handles 404 Not Found by returning 0.0, as a missing delta implies zero change.
     *
     * @param uuid The player's UUID.
     * @return A CompletableFuture that completes with the delta playtime (0.0 if not found or error),
     * or exceptionally if a critical error occurs.
     */
    public CompletableFuture<Double> getPlayerDeltaPlaytime(String uuid) {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(GAME_SERVICE_BASE_URL + "/deltatime/" + uuid))
                .GET()
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(8))
                .build();

        return HTTP_CLIENT.sendAsync(getRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    //logger.debug("GameService: Received HTTP response for delta playtime of {}. Status: {}, Body: {}", uuid, response.statusCode(), response.body());

                    // --- CRITICAL FIX START ---
                    if (response.statusCode() == 404) {
                        logger.info("GameService: Delta playtime not found for {}. Returning 0.0.", uuid);
                        return 0.0; // Return 0.0 if delta playtime data doesn't exist
                    }

                    if (response.statusCode() != 200) {
                        logger.error("GameService: Unexpected response status for delta playtime (GET {}): {}", response.statusCode(), response.body());
                        throw new RuntimeException(String.format("Unexpected response status from GameService (GET deltatime %d): %s", response.statusCode(), response.body()));
                    }

                    try {
                        DeltaPlaytimeResponse apiResponse = GSON.fromJson(response.body(), DeltaPlaytimeResponse.class);
                        return apiResponse.getDeltatime();
                    } catch (JsonSyntaxException e) {
                        logger.error("GameService: Failed to parse delta playtime JSON response for {}: {}", uuid, response.body(), e);
                        throw new RuntimeException("Failed to parse delta playtime data: " + e.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    logger.error("GameService: Exception fetching delta playtime for {}: {}", uuid, ex.getMessage(), ex);
                    return 0.0;
                });
    }
}