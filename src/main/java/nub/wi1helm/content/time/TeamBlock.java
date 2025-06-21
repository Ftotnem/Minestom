package nub.wi1helm.content.time;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.server.sound.SoundEvent;
import nub.wi1helm.server.ServerPlayer;

import java.util.Collection;
import java.util.Set;

public class TeamBlock extends TimeBlock {

    public TeamBlock(BlockVec localPos, double time) {
        // Pass a dummy block here since we override packets fully
        super(Block.AIR, localPos, time);
    }

    @Override
    public Collection<SendablePacket> getSpawnPackets(ServerPlayer player) {
        Block blockToShow;
        switch (player.getServerTeam()) {
            case AQUA_CREEPERS -> blockToShow = Block.DIAMOND_BLOCK;
            case PURPLE_AXOLOTLS -> blockToShow = Block.AMETHYST_BLOCK;
            default -> blockToShow = Block.AIR;
        }
        BlockVec absPos = new BlockVec(TimeContentUtil.transformToAbsolute(getLocalPos(), player.getServerTeam()));
        return Set.of(
                new BlockChangePacket(absPos, blockToShow),
                new SoundEffectPacket(SoundEvent.BLOCK_NOTE_BLOCK_DIDGERIDOO, Sound.Source.MASTER, absPos, 1f, 1f, 0)
        );
    }

    @Override
    public Collection<SendablePacket> getDespawnPackets(ServerPlayer player) {
        BlockVec absPos = new BlockVec(TimeContentUtil.transformToAbsolute(getLocalPos(), player.getServerTeam()));
        return Set.of(new BlockChangePacket(absPos, Block.AIR));
    }
}
