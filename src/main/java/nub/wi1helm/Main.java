package nub.wi1helm;

import net.minestom.server.MinecraftServer;

import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.instance.Instance;
import nub.wi1helm.listener.PlayerDiggingListener;
import net.minestom.server.network.packet.client.play.ClientPlayerDiggingPacket;
import nub.wi1helm.game.GameHandler;
import nub.wi1helm.listener.Global;
import nub.wi1helm.register.Registrar;
import nub.wi1helm.register.RegistrarConfig; // Import RegistrarConfig
import nub.wi1helm.server.ServerInstance;
import nub.wi1helm.server.ServerPlayer;
import nub.wi1helm.server.ServerSidebar;
import nub.wi1helm.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    public static Logger logger = LoggerFactory.getLogger(Main.class);
    public static Instance instance;
    public static Global global;
    private static Registrar registrar;

    public static void main(String[] args) {
        logger.info("Starting Minestom application...");

        MinecraftServer server = MinecraftServer.init();

        MinecraftServer.getPacketListenerManager().setPlayListener(ClientPlayerDiggingPacket.class, PlayerDiggingListener::playerDiggingListener);

        MinecraftServer.getConnectionManager().setPlayerProvider(ServerPlayer::new);
        MinecraftServer.setCompressionThreshold(0);
        VelocityProxy.enable(System.getenv().getOrDefault("VELOCITY_SECRET", "balle123"));

        instance = new ServerInstance();
        global = new Global();

        ServerSidebar.create();
        GameHandler.create();
        Template.init();

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