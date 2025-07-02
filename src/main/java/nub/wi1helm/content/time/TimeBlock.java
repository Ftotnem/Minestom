package nub.wi1helm.content.time;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import nub.wi1helm.server.ServerPlayer;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public class TimeBlock implements TimeContent {
    private final Block block;
    private final BlockVec localPos;
    private final double time;

    public TimeBlock(Block block, BlockVec localPos, double time) {
        this.block = block;
        this.localPos = localPos;
        this.time = time;
    }

    @Override
    public Collection<SendablePacket> getSpawnPackets(ServerPlayer player) {
        BlockVec abs = new BlockVec(TimeContentUtil.transformToAbsolute(this, player.getServerTeam()));
        return Set.of(new BlockChangePacket(abs, block));
    }

    @Override
    public Collection<SendablePacket> getDespawnPackets(ServerPlayer player) {
        BlockVec abs = new BlockVec(TimeContentUtil.transformToAbsolute(this, player.getServerTeam()));
        return Set.of(new BlockChangePacket(abs, Block.AIR));
    }

    @Override public double getTime() { return time; }
    @Override public BlockVec getLocalPos() { return localPos; }

}

