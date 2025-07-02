package nub.wi1helm.content.entity;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import nub.wi1helm.Main;
import nub.wi1helm.content.time.TimeContent;
import nub.wi1helm.content.time.TimeContentUtil;
import nub.wi1helm.server.ServerPlayer;

import java.util.Collection;
import java.util.List;

public class SignTimeEntity extends Entity implements TimeContent {

    private Pos local;
    private double time;

    public SignTimeEntity(Instance instance, Pos pos, double time, Component text, Vec scale) {
        super(EntityType.TEXT_DISPLAY);
        setInstance(instance);
        setAutoViewable(false);
        editEntityMeta(TextDisplayMeta.class, meta -> {
            meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.VERTICAL);
            meta.setText(text);
            meta.setViewRange(200);
            meta.setBackgroundColor(16711680); // Red color for background, adjust as needed
            meta.setHasNoGravity(false);
            meta.setScale(scale);
        });
        this.time = time;
        this.local = pos;

    }

    @Override
    public double getTime() {
        return time;
    }

    @Override
    public Point getLocalPos() {
        return local;
    }

    @Override
    public Collection<SendablePacket> getSpawnPackets(ServerPlayer player) {

        Pos absolutePos = new Pos(TimeContentUtil.transformToAbsolute(this, player.getServerTeam()));
        setPositionInternal(absolutePos); // Update the NPC's server-side position

        return List.of(getSpawnPacket(), getMetadataPacket());
    }

    @Override
    public Collection<SendablePacket> getDespawnPackets(ServerPlayer player) {
        return List.of(new DestroyEntitiesPacket(getEntityId()));
    }
}
