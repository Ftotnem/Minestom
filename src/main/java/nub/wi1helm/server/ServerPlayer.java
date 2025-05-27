package nub.wi1helm.server;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import nub.wi1helm.player.GameService;
import nub.wi1helm.player.PlayerService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;


import static nub.wi1helm.Main.logger;

public class ServerPlayer extends Player {

    private ServerProfile serverProfile;

    private final PlayerService playerService = new PlayerService();
    private final GameService gameService = new GameService();

    private final CompletableFuture<Void> playerDataLoadFuture;

    public ServerPlayer(@NotNull PlayerConnection playerConnection, @NotNull GameProfile gameProfile) {
        super(playerConnection, gameProfile);

        this.playerDataLoadFuture = playerService.loadPlayerProfile(getUuid().toString(), getUsername())
                .thenCompose(loadedProfile -> {
                    this.serverProfile = loadedProfile;

                    // Initial load of playtime data from GameService (Redis)
                    // This fetches the *current* state at login.
                    CompletableFuture<Double> totalPlaytimeFuture = gameService.getPlayerTotalPlaytime(getUuid().toString());
                    CompletableFuture<Double> deltaPlaytimeFuture = gameService.getPlayerDeltaPlaytime(getUuid().toString());

                    return CompletableFuture.allOf(totalPlaytimeFuture, deltaPlaytimeFuture)
                            .thenAccept(v -> {
                                try {
                                    double totalPlaytime = totalPlaytimeFuture.join();
                                    double deltaPlaytime = deltaPlaytimeFuture.join();

                                    this.serverProfile.setPlaytime(totalPlaytime);
                                    this.serverProfile.setDeltaPlaytime(deltaPlaytime);

                                    logger.info("ServerPlayer {} (UUID: {}) fully initialized with profile: Team={}, TotalPlaytime={}, DeltaPlaytime={}, Banned={}, FirstJoin={}",
                                            getUsername(), getUuid(), serverProfile.getServerTeam(), serverProfile.getPlaytime(), serverProfile.getDeltaPlaytime(), serverProfile.isBanned(), serverProfile.isFirstJoin());

                                } catch (Exception e) {
                                    logger.error("Failed to fetch initial playtime data for {} (UUID: {}): {}", getUsername(), getUuid(), e.getMessage(), e);
                                    // Player can still join, just with initial playtime possibly being 0.0 if fetch failed
                                }
                            });
                })
                .exceptionally(ex -> {
                    logger.error("Failed to load/create ServerProfile for {} (UUID: {}): {}", getUsername(), getUuid(), ex.getMessage(), ex);
                    Throwable actualCause = ex.getCause() != null ? ex.getCause() : ex;
                    this.kick(Component.text("Failed to load your player data: " + actualCause.getMessage()));
                    return null;
                });
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        // Fetch the latest total playtime from the Go service (Redis) on EVERY tick.
        // This ensures the in-game data in serverProfile is updated with real-time Redis values.
        gameService.getPlayerTotalPlaytime(getUuid().toString())
                .thenAccept(latestPlaytime -> {
                    if (serverProfile != null) {
                        serverProfile.setPlaytime(latestPlaytime);
                    }
                })
                .exceptionally(e -> {
                    logger.warn("Failed to fetch latest total playtime for {}: {}", getUuid(), e.getMessage());
                    return null; // Handle exception gracefully
                });

        // Fetch the latest delta playtime from the Go service (Redis) on EVERY tick.
        gameService.getPlayerDeltaPlaytime(getUuid().toString())
                .thenAccept(latestDeltaPlaytime -> {
                    if (serverProfile != null) {
                        serverProfile.setDeltaPlaytime(latestDeltaPlaytime);
                    }
                })
                .exceptionally(e -> {
                    logger.warn("Failed to fetch latest delta playtime for {}: {}", getUuid(), e.getMessage());
                    return null; // Handle exception gracefully
                });

        updateActionBar();

    }

    public void updateActionBar() {
        long seconds = (long) (serverProfile.getPlaytime() / 20);
        String formattedTime = String.format("%08d", seconds);
        String displayTime = formattedTime.replaceAll("(?<=\\d)(?=(\\d{3})+$)", ".");

        // ticksPertick directly equals seconds gained per second
        String rate = String.format("<gray>+%.1fs/s</gray>", (double) getServerProfile().getDeltaPlaytime());

        String full = displayTime + " " + rate;
        this.sendActionBar(MiniMessage.miniMessage().deserialize(full));
    }
    // Getter for the CompletableFuture to check when data is loaded
    public CompletableFuture<Void> getPlayerDataLoadFuture() {
        return playerDataLoadFuture;
    }

    @NotNull
    public ServerProfile getServerProfile() {
        if (serverProfile == null) {
            logger.warn("Attempted to access ServerProfile for {} before it was loaded!", getUsername());
        }
        return serverProfile;
    }

    // These getters now correctly reflect the data stored in serverProfile,
    // which is updated on every tick from Redis.
    public double getPlaytime() {
        return serverProfile != null ? serverProfile.getPlaytime() : 0.0;
    }
    public double getDeltaPlaytime() {
        return serverProfile != null ? serverProfile.getDeltaPlaytime() : 1.0;
    }

    public boolean isBanned() {
        return serverProfile != null && serverProfile.isBanned();
    }

    public ServerTeam getServerTeam() {
        return serverProfile != null ? serverProfile.getServerTeam() : ServerTeam.UNKNOWN;
    }
}