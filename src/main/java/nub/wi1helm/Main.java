package nub.wi1helm;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.instance.Instance;
import nub.wi1helm.game.GameHandler;
import nub.wi1helm.register.Registrar;
import nub.wi1helm.register.RegistrarConfig; // Import RegistrarConfig
import nub.wi1helm.server.ServerInstance;
import nub.wi1helm.server.ServerPlayer;
import nub.wi1helm.server.ServerSidebar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    public static Logger logger = LoggerFactory.getLogger(Main.class);
    public static Instance instance;
    private static Registrar registrar;

    public static void main(String[] args) {
        logger.info("Starting Minestom application...");

        MinecraftServer server = MinecraftServer.init();
        MinecraftServer.getConnectionManager().setPlayerProvider(ServerPlayer::new);
        MinecraftServer.setCompressionThreshold(0);
        VelocityProxy.enable(System.getenv().getOrDefault("VELOCITY_SECRET", "balle123"));

        instance = new ServerInstance();
        ServerSidebar.create();
        GameHandler.create();

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final ServerPlayer player = (ServerPlayer) event.getPlayer();
            event.setSpawningInstance(instance);
            player.setGameMode(GameMode.SPECTATOR);
        });

        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> {
            final ServerPlayer player = (ServerPlayer) event.getPlayer();
            if (player.getServerProfile().isFirstJoin()) {
                GameHandler.playTeamSelectAnimation(player, player.getServerTeam());
            }
            player.sendMessage(player.getServerTeam().name());
        });

        globalEventHandler.addListener(PlayerDisconnectEvent.class, event -> {
            final ServerPlayer player = (ServerPlayer) event.getPlayer();
        });

        try {
            // Choose configuration based on an environment variable, e.g., "ENVIRONMENT=dev"
            RegistrarConfig config;
            if ("dev".equalsIgnoreCase(System.getenv("ENVIRONMENT"))) {
                config = RegistrarConfig.loadDevConfig();
            } else {
                config = RegistrarConfig.loadFromEnv();
            }

            // Now pass the chosen config to Registrar.createAndConfigure
            // Note: Registrar's createAndConfigure method needs to be updated to accept RegistrarConfig
            // I will update Registrar.java accordingly below to reflect this.
            registrar = Registrar.createAndConfigure(config); // This line will change slightly
            registrar.start();
            logger.info("Registrar started successfully for Minestom server: {}:{} (Label: {})",
                    registrar.getMinestomPodIp(), registrar.getMinestomPort(), registrar.getMinestomServerLabel());
        } catch (IllegalStateException e) {
            logger.error("Failed to initialize or start the Registrar: {}", e.getMessage(), e);
            return;
        } catch (Exception e) {
            logger.error("An unexpected error occurred while starting the Registrar: {}", e.getMessage(), e);
            return;
        }

        int minestomListeningPort = registrar.getMinestomPort();
        server.start("0.0.0.0", minestomListeningPort);
        logger.info("Minestom server listening on {}:{}", registrar.getMinestomPodIp(), minestomListeningPort);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered. Stopping Minestom server and Registrar...");
            if (registrar != null) {
                registrar.stop();
            }
            logger.info("Application shutdown complete.");
        }));
    }
}