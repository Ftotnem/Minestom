package nub.wi1helm.game;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.TaskSchedule;
import nub.wi1helm.Main;
import nub.wi1helm.player.GameService;
import nub.wi1helm.server.ServerPlayer;
import nub.wi1helm.server.ServerTeam;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static nub.wi1helm.Main.logger;

public class GameHandler {

    private static final Map<ServerTeam, Entity> teamPlaytimeDisplays = new EnumMap<>(ServerTeam.class);
    // Make latestTeamPlaytimes accessible for CustomPlaytimeTextDisplay
    private static final Map<ServerTeam, Double> latestTeamPlaytimes = new ConcurrentHashMap<>();

    private static final GameService gameServiceClient = GameService.getInstance();

    private static final ScheduledExecutorService dataFetchScheduler = Executors.newSingleThreadScheduledExecutor();
    private static final long DATA_FETCH_INTERVAL_SECONDS = 100;

    /**
     * Initializes game-related cosmetic elements, such as the team playtime billboards.
     */
    public static void create() {
        for (ServerTeam team : ServerTeam.values()) {
            latestTeamPlaytimes.put(team, 0.0); // Initialize to 0.0
        }

        // Create Text Display for AQUA_CREEPERS using the separate class
        Entity creeperDisplay = new CustomPlaytimeTextDisplay(ServerTeam.AQUA_CREEPERS);
        creeperDisplay.setInstance(Main.instance, new Pos(-24.0, -32, 8.0, -90, 0));
        teamPlaytimeDisplays.put(ServerTeam.AQUA_CREEPERS, creeperDisplay);

        // Create Text Display for PURPLE_SWORDERS using the separate class
        Entity swordersDisplay = new CustomPlaytimeTextDisplay(ServerTeam.PURPLE_SWORDERS);
        swordersDisplay.setInstance(Main.instance, new Pos(40.0, -32, 8.0, 90, 0));
        teamPlaytimeDisplays.put(ServerTeam.PURPLE_SWORDERS, swordersDisplay);

        // Schedule periodic updates for the local data cache
        dataFetchScheduler.scheduleAtFixedRate(GameHandler::fetchAndUpdateLocalPlaytimeCache,
                0, DATA_FETCH_INTERVAL_SECONDS, TimeUnit.MILLISECONDS);

        logger.info("GameHandler: Initialized team playtime billboards and scheduled data fetch every {} seconds.", DATA_FETCH_INTERVAL_SECONDS);
    }

    /**
     * Provides access to the latest team playtimes cache for external classes like CustomPlaytimeTextDisplay.
     * @return The map containing the latest team playtimes.
     */
    public static Map<ServerTeam, Double> getLatestTeamPlaytimes() {
        return latestTeamPlaytimes;
    }

    /**
     * Fetches the latest team playtime data from the Game Service and updates the local cache.
     * This method runs periodically.
     */
    private static void fetchAndUpdateLocalPlaytimeCache() {
        for (ServerTeam team : ServerTeam.values()) {
            gameServiceClient.getTeamPlaytime(team.name())
                    .thenAccept(totalPlaytime -> {
                        latestTeamPlaytimes.put(team, totalPlaytime);
                        // logger.info("GameHandler: Updated local cache for team {} to {} ticks.", team.name(), totalPlaytime); // Uncomment for debugging
                    })
                    .exceptionally(ex -> {
                        logger.error("GameHandler: Failed to fetch playtime for team {} for local cache: {}", team.name(), ex.getMessage(), ex);
                        // Keep the old value in cache if fetch fails
                        return null;
                    });
        }
    }

    /**
     * Plays the team selection animation for a player.
     *
     * @param player The ServerPlayer instance.
     * @param selectedTeam The team that the player will be assigned to after the animation.
     */
    public static void playTeamSelectAnimation(ServerPlayer player, ServerTeam selectedTeam) {
        final Queue<ServerTeam> choices = new ArrayDeque<>(List.of(ServerTeam.AQUA_CREEPERS, ServerTeam.PURPLE_SWORDERS));

        double currentDelaySeconds = 1.0;
        final double decrementSeconds = 0.05;
        final double minimumDelaySeconds = 0.05;
        final int minimumIterationsAtFastSpeed = 8;
        int totalTicks = 0;

        ServerTeam firstTeam = choices.poll();
        player.showTitle(Title.title(
                Component.text("Your Team Is.."),
                Component.empty()
        ));
        player.playSound(Sound.sound().source(Sound.Source.MASTER).type(Key.key("block.note_block.bell")).build());
        choices.offer(firstTeam);

        int fastIterationsCount = 0;
        while (currentDelaySeconds >= minimumDelaySeconds || fastIterationsCount < minimumIterationsAtFastSpeed) {
            int delayTicks = (int)(Math.max(currentDelaySeconds, minimumDelaySeconds) * 20);
            totalTicks += delayTicks;

            if (currentDelaySeconds <= minimumDelaySeconds) {
                fastIterationsCount++;
            }

            ServerTeam displayTeam = choices.poll();
            final ServerTeam teamToShow = displayTeam;

            MinecraftServer.getSchedulerManager().scheduleTask(() -> {
                player.showTitle(Title.title(
                        Component.text(teamToShow.displayName()).color(teamToShow.color()),
                        Component.empty()
                ));
                player.playSound(Sound.sound().source(Sound.Source.MASTER).type(Key.key("block.note_block.bell")).build());
                return null;
            }, TaskSchedule.tick(totalTicks));

            choices.offer(displayTeam);

            if (currentDelaySeconds > minimumDelaySeconds) {
                currentDelaySeconds -= decrementSeconds;
            }
        }

        final int finalDelay = totalTicks + 10;
        MinecraftServer.getSchedulerManager().scheduleTask(() -> {
            player.showTitle(Title.title(
                    Component.text(selectedTeam.displayName()).color(selectedTeam.color()),
                    Component.empty()
            ));
            return null;
        }, TaskSchedule.tick(finalDelay));
    }

    /**
     * Calculates the estimated total duration of the team selection animation in milliseconds.
     * This is an approximation used by ServerTeamHandler to schedule the actual team assignment.
     */
    public static long getAnimationDurationMillis() {
        double currentDelaySeconds = 1.0;
        final double decrementSeconds = 0.05;
        final double minimumDelaySeconds = 0.05;
        final int minimumIterationsAtFastSpeed = 8;
        double totalDurationSeconds = 0;

        int fastIterationsCount = 0;
        while (currentDelaySeconds >= minimumDelaySeconds || fastIterationsCount < minimumIterationsAtFastSpeed) {
            double delay = Math.max(currentDelaySeconds, minimumDelaySeconds);
            totalDurationSeconds += delay;

            if (currentDelaySeconds <= minimumDelaySeconds) {
                fastIterationsCount++;
            }

            if (currentDelaySeconds > minimumDelaySeconds) {
                currentDelaySeconds -= decrementSeconds;
            }
        }
        totalDurationSeconds += (10.0 / 20.0); // 10 ticks buffer

        return (long) (totalDurationSeconds * 1000);
    }

    /**
     * Shuts down the internal scheduler used for periodic tasks.
     */
    public static void shutdown() {
        dataFetchScheduler.shutdown();
        try {
            if (!dataFetchScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("GameHandler data fetch scheduler did not terminate in time, forcing shutdown.");
                dataFetchScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("GameHandler data fetch scheduler termination interrupted: {}", e.getMessage());
            dataFetchScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("GameHandler: Shut down data fetch scheduler.");
    }
}