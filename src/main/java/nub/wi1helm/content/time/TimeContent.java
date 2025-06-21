package nub.wi1helm.content.time;

import net.minestom.server.coordinate.Point;
import net.minestom.server.network.packet.server.SendablePacket;
import nub.wi1helm.server.ServerPlayer;

import java.util.Collection;

public interface TimeContent {
    double getTime(); // When to appear
    Point getLocalPos();
    Collection<SendablePacket> getSpawnPackets(ServerPlayer player);
    Collection<SendablePacket> getDespawnPackets(ServerPlayer player);

    // New method to check if the content can spawn for the given player
    default boolean canSpawn(ServerPlayer player) {
        return TimeContentUtil.checkTime(player.getCurrentPlaytime(), getTime());
    }
}