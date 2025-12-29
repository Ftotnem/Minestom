package nub.wi1helm.content.npc;

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
import nub.wi1helm.inventory.TimeExchangeBankMenu;
import nub.wi1helm.server.ServerPlayer;
import nub.wi1helm.server.ServerTeam;
import nub.wi1helm.template.npc.Interactable;
import nub.wi1helm.template.npc.Nameable;
import nub.wi1helm.template.npc.actions.AbstractAction;
import nub.wi1helm.template.npc.actions.ActionList;
import nub.wi1helm.template.npc.actions.DynamicMenuAction;
import nub.wi1helm.template.npc.player.SkinLayer;
import nub.wi1helm.template.npc.player.TemplatePlayerNPC;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TimeExchangeNPC extends TemplatePlayerNPC implements TimeContent, Nameable, Interactable {

    private Pos localPos;
    private double time;
    private ActionList actionList = ActionList.empty(); // Initialize with an empty list

    public TimeExchangeNPC(Instance instance, Pos pos, double time) {
        super(instance);
        this.localPos = pos;
        this.time = time;

        // Register the interaction listener when the NPC is created
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

    /**
     * Personalizes the NPC for a specific player, setting skin, name, and defining the interaction action list.
     * When interacted with, this NPC will open the time exchange shop interface.
     *
     * @param player The player for whom to personalize the NPC.
     */
    @Override
    public void personalize(Player player) {
        final ServerPlayer p = (ServerPlayer) player;
        setSkinLayer(SkinLayer.FULL);
        setSkin(p.getSkin()); // Use player's skin for personalization

        String name = "Time Master"; // Default name
        if (p.getServerTeam() == ServerTeam.AQUA_CREEPERS) {
            name = "<" + ServerTeam.AQUA_CREEPERS.color() + ">Chrono</" + ServerTeam.AQUA_CREEPERS.color() + ">";
        }
        if (p.getServerTeam() == ServerTeam.PURPLE_AXOLOTLS) {
            name = "<" + ServerTeam.PURPLE_AXOLOTLS.color() + ">Tempus</" + ServerTeam.PURPLE_AXOLOTLS.color() + ">";
        }

        // Set the NPC's name, including the hologram above its head.
        setName(Main.instance, MiniMessage.miniMessage().deserialize("<dark_purple>[BANK]</dark_purple> " + name), MiniMessage.miniMessage().deserialize("<gold><bold>CLICK TO EXCHANGE TIME!</bold></gold>"));

        // Define the action to open the shop using MenuAction.
        setActionList(new ActionList(
                new DynamicMenuAction(0, 0, true, new TimeExchangeBankMenu()) { // Action 0 directly opens the menu
                    @Override
                    public AbstractAction determineNextAction(Player player, ActionList actionList) {
                        // After the menu is closed, you might want to return to this same action
                        // so that the next click re-opens the menu.
                        return actionList.getAction(0);
                    }
                }
        ));
    }

    /**
     * Returns the collection of packets required to spawn this NPC for a specific player.
     * This includes packets for the NPC entity itself and its name hologram.
     *
     * @param player The player for whom to get the spawn packets.
     * @return A collection of SendablePacket objects.
     */
    @Override
    public @NotNull Collection<SendablePacket> getSpawnPackets(@NotNull ServerPlayer player) {
        // Calculate the absolute position based on local position and player's team.
        Pos absolutePos = new Pos(TimeContentUtil.transformToAbsolute(this, player.getServerTeam()));
        setPositionInternal(absolutePos); // Update the NPC's server-side position

        List<SendablePacket> packets = new ArrayList<>();
        packets.addAll(getNpcSpawnPackets(player));
        packets.addAll(getNameSpawnPackets(player));
        return packets;
    }

    /**
     * Returns the collection of packets required to despawn this NPC for a specific player.
     * This includes packets to destroy the NPC entity and its name hologram.
     *
     * @param player The player for whom to get the despawn packets.
     * @return A collection of SendablePacket objects.
     */
    @Override
    public @NotNull Collection<SendablePacket> getDespawnPackets(@NotNull ServerPlayer player) {
        List<SendablePacket> packets = new ArrayList<>();
        packets.addAll(getNpcDespawnPackets(player));
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

    /**
     * Sets up the global event listener for player interaction with this NPC.
     * It ensures the shop actions are triggered when the NPC is clicked.
     */
    public void interact() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerEntityInteractEvent.class, event -> {
            // Ignore off-hand interactions
            if (event.getHand().equals(PlayerHand.OFF)) return;
            // Check if the interacted entity is this NPC
            if (event.getTarget().getUuid().equals(this.getUuid())) {
                onInteract(event.getPlayer()); // Trigger the interaction logic
            }
        });
    }
}