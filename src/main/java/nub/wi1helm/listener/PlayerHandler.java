package nub.wi1helm.listener;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.*;
import net.minestom.server.event.trait.PlayerEvent;
import nub.wi1helm.inventory.DefaultInventory;
import nub.wi1helm.server.ServerPlayer;
import nub.wi1helm.server.ServerTeam;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;

public class PlayerHandler {

    EventNode<PlayerEvent> node = EventNode.type("player", EventFilter.PLAYER);

    static PlayerHandler instance;

    private PlayerHandler(EventNode<Event> global) {
        global.addChild(this.node);

        join();
        leave();
        cancel();
        voidPushBack();
        voidTeleport();
    }

    @NotNull
    public static PlayerHandler getInstance(EventNode<Event> global) {
        if (instance == null) return new PlayerHandler(global);
        return instance;
    }

    @Nullable
    public static PlayerHandler getInstance() {
        return instance;
    }

    private void voidPushBack() {
        node.addListener(PlayerMoveEvent.class, event -> {

            final ServerPlayer player = (ServerPlayer) event.getPlayer();

            if (player.getPosition().y() > -62) return;

            ServerTeam team = player.getServerTeam();

            Pos target = team.getPos();
            Pos position = player.getPosition();

            Vec vector = target.asVec().sub(position);

            player.setVelocity(vector.withY(30));


        });
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

            player.sendPlayerListHeaderAndFooter(MiniMessage.miniMessage().deserialize("<gradient:#00d2ff:#3a47d5><bold>Race To 10 Million</bold></gradient>"), MiniMessage.miniMessage().deserialize("<gradient:#3a47d5:#00d2ff>shop.raceto10m.com</gradient>"));
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
