package nub.wi1helm.content.npc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.SendablePacket;
import nub.wi1helm.Main;
import nub.wi1helm.content.time.TimeContent;
import nub.wi1helm.content.time.TimeContentUtil;
import nub.wi1helm.server.ServerPlayer;
import nub.wi1helm.template.npc.Interactable;
import nub.wi1helm.template.npc.Nameable;
import nub.wi1helm.template.npc.actions.AbstractAction;
import nub.wi1helm.template.npc.actions.ActionList;
import nub.wi1helm.template.npc.actions.DialogAction;
import nub.wi1helm.template.npc.player.SkinLayer;
import nub.wi1helm.template.npc.player.TemplatePlayerNPC;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TutorialNPC extends TemplatePlayerNPC implements TimeContent, Nameable, Interactable {

    private Pos localPos;
    private double time;
    private ActionList actionList = ActionList.empty();

    public TutorialNPC(Instance instance, Pos pos, double time) {
        super(instance);
        this.localPos = pos;
        this.time = time;

        interact();
    }

    @Override
    public double getTime() {
        return this.time;
    }

    @Override
    public Point getLocalPos() {
        return this.localPos;
    }

    @Override
    public void personalize(Player player) {
        setSkinLayer(SkinLayer.FULL);
        setSkin(player.getSkin());
        // Call setName here to perform the server-side setup of the hologram.
        // It does NOT return packets anymore, it just sets up the entity structure.
        setName(Main.instance, Component.text("wi1helm_"), Component.text("A Fucking NooB"));


        setActionList(new ActionList(
                // Action 0: Entry point for ANY click on this NPC
                new AbstractAction(0, 0, true) {
                    @Override
                    public void execute(Player player) {
                        // No immediate output needed here
                    }

                    @Override
                    public AbstractAction determineNextAction(Player player, ActionList actionList) {
                        return actionList.getAction(1);
                    }
                },

                // Action 1,2,3: The conversation lines
                new DialogAction(1, 0, false, MiniMessage.miniMessage().deserialize("<grey>[<yellow>NPC</yellow>]</grey> <white>Fish Merchant</white>: Hello")) {
                    @Override
                    public AbstractAction determineNextAction(Player player, ActionList actionList) {
                        return actionList.getAction(2);
                    }
                },
                new DialogAction(2, 2000, false, MiniMessage.miniMessage().deserialize("<grey>[<yellow>NPC</yellow>]</grey> <white>Fish Merchant</white>: I sell fish themed items")) {
                    @Override
                    public AbstractAction determineNextAction(Player player, ActionList actionList) {
                        return actionList.getAction(3);
                    }
                },
                new DialogAction(3, 2000, false, MiniMessage.miniMessage().deserialize("<grey>[<yellow>NPC</yellow>]</grey> <white>Fish Merchant</white>: Click me again to open my shop")) {
                    @Override
                    public AbstractAction determineNextAction(Player player, ActionList actionList) {
                        // after the last line, jump back to action 0
                        // so that the next click restarts the logic
                        return actionList.getAction(0);
                    }
                }
        ));
    }

    @Override
    public @NotNull Collection<SendablePacket> getSpawnPackets(@NotNull ServerPlayer player) {
        // Calculate the absolute position based on local position and player's team.
        Pos absolutePos = new Pos(TimeContentUtil.transformToAbsolute(localPos, player.getServerTeam()));
        setPositionInternal(absolutePos); // Update the NPC's server-side position

        // Initialize a list to hold all packets to be sent.
        List<SendablePacket> packets = new ArrayList<>();

        // 1. Get the base NPC's spawn packets.
        // This includes SpawnEntityPacket, EntityMetaDataPacket for the NPC itself.
        packets.addAll(getNpcSpawnPackets(player)); // Assuming TemplatePlayerNPC has this method

        // 2. Get the name hologram's spawn packets.
        // This method specifically returns the packets required for the client to see the hologram.
        packets.addAll(getNameSpawnPackets(player));

        return packets;
    }

    @Override
    public @NotNull Collection<SendablePacket> getDespawnPackets(@NotNull ServerPlayer player) {
        List<SendablePacket> packets = new ArrayList<>();

        // Get the base NPC's despawn packets.
        packets.addAll(getNpcDespawnPackets(player)); // Assuming TemplatePlayerNPC has this method

        // Get the name hologram's despawn packets.
        // This will retrieve the packets to destroy the hologram entities.
        packets.addAll(getNameDespawnPackets(player));

        return packets;
    }

    @Override
    public @NotNull ActionList getActionList() {
        return actionList;
    }

    @Override
    public void setActionList(@NotNull ActionList actionList) {
        this.actionList = actionList;
    }

    public void interact() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerEntityInteractEvent.class, event -> {
            if (event.getHand().equals(PlayerHand.OFF)) return;
            if (event.getTarget().getUuid() == this.getUuid()) onInteract(event.getPlayer());
        });
    }
}