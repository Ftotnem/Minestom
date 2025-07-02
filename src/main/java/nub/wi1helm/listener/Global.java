package nub.wi1helm.listener;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import nub.wi1helm.Main;
import nub.wi1helm.server.ServerPlayer;

public class Global{

    EventNode<Event> node = MinecraftServer.getGlobalEventHandler();

    InstanceHandler instanceListener = InstanceHandler.getInstance(node);
    PlayerHandler playerHandler = PlayerHandler.getInstance(node);
    ChatHandler chatHandler = ChatHandler.getInstance(node);
    public Global() {

        loadPlayer();
    }


    private void loadPlayer() {
        node.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final ServerPlayer player = (ServerPlayer) event.getPlayer();
            event.setSpawningInstance(Main.instance);
        });
    }

}
