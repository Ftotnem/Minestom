package nub.wi1helm.listener;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.*;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.listener.PlayerDiggingListener;
import nub.wi1helm.game.GameHandler;
import nub.wi1helm.inventory.DefaultInventory;
import nub.wi1helm.server.ServerPlayer;
import nub.wi1helm.server.ServerTeam;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;

public class PlayerListener {

    EventNode<PlayerEvent> node = EventNode.type("instance", EventFilter.PLAYER);

    static PlayerListener instance;

    private PlayerListener(EventNode<Event> global) {
        global.addChild(this.node);

        join();
        leave();
        cancel();
        voidTeleport();
    }

    @NotNull
    public static PlayerListener getInstance(EventNode<Event> global) {
        if (instance == null) return new PlayerListener(global);
        return instance;
    }

    @Nullable
    public static PlayerListener getInstance() {
        return instance;
    }

    private void voidTeleport() {
        node.addListener(PlayerMoveEvent.class, event -> {

            final ServerPlayer player = (ServerPlayer) event.getPlayer();

            if (player.getPosition().y() > -64) return;

            ServerTeam team = player.getServerTeam();

            player.teleport(team.getPos());
        });
    }

    private void join() {
        node.addListener(PlayerSpawnEvent.class, event -> {
            final ServerPlayer player = (ServerPlayer) event.getPlayer();
            player.applyInitialSpawnLogic();
            new DefaultInventory().constructPlayerInventory(player);
        });
    }

    private void leave() {
        node.addListener(PlayerDisconnectEvent.class, event -> {
            final ServerPlayer player = (ServerPlayer) event.getPlayer();

        });
    }


    private void cancel(){
        node.addListener(PlayerBlockBreakEvent.class, event -> {
           event.setCancelled(true);
        });
    }
}
