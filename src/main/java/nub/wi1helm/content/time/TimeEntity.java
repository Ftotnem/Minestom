package nub.wi1helm.content.time;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import nub.wi1helm.Main;
import nub.wi1helm.server.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Base class for entities that are part of the time-based content system.
 * This class extends Minestom's Entity and implements TimeContent,
 * providing the necessary methods for position transformation and generating
 * the core entity spawn and metadata packets.
 */
public class TimeEntity extends Entity implements TimeContent {

    private final Double time;
    private final Pos localPos;

    public TimeEntity(@NotNull EntityType entityType, Pos localPos, Double time) {
        super(entityType);
        this.time = time;
        this.localPos = localPos;
        setAutoViewable(false);
        // The initial position will be updated dynamically per player on view.
    }

    @Override
    public Collection<SendablePacket> getSpawnPackets(ServerPlayer player) {
        List<SendablePacket> packets = new ArrayList<>();

        Pos absolutePos = new Pos(TimeContentUtil.transformToAbsolute(this, player.getServerTeam()));
        this.setInstance(Main.instance, absolutePos);

        SpawnEntityPacket spawnPacket = getSpawnPacket();
        EntityMetaDataPacket metaDataPacket = getMetadataPacket();

        packets.add(spawnPacket);
        packets.add(metaDataPacket);

        return packets;
    }

    @Override
    public Collection<SendablePacket> getDespawnPackets(ServerPlayer player) {
        return List.of(new DestroyEntitiesPacket(getEntityId()));
    }

    @Override
    public double getTime() {
        return time;
    }

    @Override
    public Point getLocalPos() {
        return localPos;
    }
}
