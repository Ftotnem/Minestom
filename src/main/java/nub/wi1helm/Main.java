package nub.wi1helm;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.instance.Instance;
import nub.wi1helm.server.ServerInstance;
import nub.wi1helm.server.ServerPlayer;
import nub.wi1helm.server.ServerSidebar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    public static Logger logger = LoggerFactory.getLogger(Main.class);
    public static Instance instance;

    public static void main(String[] args) {

        MinecraftServer server = MinecraftServer.init();
        MinecraftServer.getConnectionManager().setPlayerProvider(ServerPlayer::new);
        MinecraftServer.setCompressionThreshold(0);
        VelocityProxy.enable("balle123");


        instance = new ServerInstance();

        ServerSidebar.create();

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final ServerPlayer player = (ServerPlayer) event.getPlayer();

            event.setSpawningInstance(instance);
            player.setGameMode(GameMode.SPECTATOR);

        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, event -> {
            final ServerPlayer player = (ServerPlayer) event.getPlayer();
            //TabUtils.setPlayerTab(player);
            player.sendMessage(player.getServerTeam().name());
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerDisconnectEvent.class, event -> {
            final ServerPlayer player = (ServerPlayer) event.getPlayer();
            //TabUtils.setPlayerTab(player);
        });

        server.start("0.0.0.0",25566);


    }
}