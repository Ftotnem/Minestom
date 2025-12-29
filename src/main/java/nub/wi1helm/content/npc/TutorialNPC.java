package nub.wi1helm.content.npc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
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
import nub.wi1helm.server.ServerTeam;
import nub.wi1helm.template.npc.Interactable;
import nub.wi1helm.template.npc.Nameable;
import nub.wi1helm.template.npc.actions.AbstractAction;
import nub.wi1helm.template.npc.actions.ActionList;
import nub.wi1helm.template.npc.actions.DialogAction;
import nub.wi1helm.template.npc.actions.TitleAction; // Ensure this is imported correctly
import nub.wi1helm.template.npc.player.SkinLayer;
import nub.wi1helm.template.npc.player.TemplatePlayerNPC;
import org.jetbrains.annotations.NotNull;

import java.time.Duration; // Import Duration for Title.Times
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TutorialNPC extends TemplatePlayerNPC implements TimeContent, Nameable, Interactable {

    private Pos localPos;
    private double time;
    private ActionList actionList = ActionList.empty(); // Initialize with an empty list

    public TutorialNPC(Instance instance, Pos pos, double time) {
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
     * Personalizes the NPC for a specific player, setting skin, name, and defining the tutorial action list.
     * The tutorial uses TitleActions for most messages and one DialogAction for variety.
     *
     * @param player The player for whom to personalize the NPC.
     */
    @Override
    public void personalize(Player player) {
        final ServerPlayer p = (ServerPlayer) player;
        setSkinLayer(SkinLayer.FULL);
        setSkin(p.getSkin());

        String name = "Tutorial";
        if (p.getServerTeam() == ServerTeam.AQUA_CREEPERS) {
            name = "<" + ServerTeam.AQUA_CREEPERS.color() + ">Creeper0</" + ServerTeam.AQUA_CREEPERS.color() + ">";
        }
        if (p.getServerTeam() == ServerTeam.PURPLE_AXOLOTLS) {
            name = "<" + ServerTeam.PURPLE_AXOLOTLS.color() + ">Axolotl0</" + ServerTeam.PURPLE_AXOLOTLS.color() + ">";
        }

        // Set the NPC's name, which includes setting up the hologram above its head.
        setName(Main.instance, MiniMessage.miniMessage().deserialize("<gold>[NPC]</gold> " + name), MiniMessage.miniMessage().deserialize("<yellow><bold>CLICK FOR TUTORIAL!</bold></yellow>"));

        // Define the sequence of actions for the tutorial
        setActionList(new ActionList(
                // Action 0: Entry point for ANY click on this NPC
                new AbstractAction(0, 0, true) {
                    @Override
                    public void execute(Player player) {
                        // No immediate output needed for the entry point
                    }

                    @Override
                    public AbstractAction determineNextAction(Player player, ActionList actionList) {
                        return actionList.getAction(1); // Proceed to the first tutorial message
                    }
                },

                // Action 1: Welcome Title - Introduces the player to the game
                new TitleAction(1, 0, false,
                        Title.title(
                                MiniMessage.miniMessage().deserialize("<yellow>Welcome!</yellow>"),
                                MiniMessage.miniMessage().deserialize("<gray>Learn how to play!</gray>"),
                                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))
                        )) {
                    @Override
                    public AbstractAction determineNextAction(Player player, ActionList actionList) {
                        return actionList.getAction(2); // Next action in sequence
                    }
                },

                // Action 2: Goal Explanation Title - Explains the main objective
                new TitleAction(2, 4000, false, // Delay after previous action completes
                        Title.title(
                                MiniMessage.miniMessage().deserialize("<green>Your Team's Goal:</green>"),
                                MiniMessage.miniMessage().deserialize("<white>Reach <gold>10 Million Seconds</gold> Total Playtime!</white>"),
                                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(4000), Duration.ofMillis(1000))
                        )) {
                    @Override
                    public AbstractAction determineNextAction(Player player, ActionList actionList) {
                        return actionList.getAction(3); // Next action in sequence
                    }
                },

                // Action 3: Content Unlock Title - Explains how content appears
                new TitleAction(3, 5000, false, // Delay after previous action completes
                        Title.title(
                                MiniMessage.miniMessage().deserialize("<aqua>Content Unlocks with Playtime!</aqua>"),
                                MiniMessage.miniMessage().deserialize("<white>New features appear as you play!</white>"),
                                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(4000), Duration.ofMillis(1000))
                        )) {
                    @Override
                    public AbstractAction determineNextAction(Player player, ActionList actionList) {
                        return actionList.getAction(4); // Next action in sequence
                    }
                },

                // Action 4: Example and Exploration Title - Gives a specific example and encourages exploration
                new TitleAction(4, 5000, false, // Delay after previous action completes
                        Title.title(
                                MiniMessage.miniMessage().deserialize("<light_purple>Example:</light_purple>"),
                                MiniMessage.miniMessage().deserialize("<white>Emerald Block at <green>50 Seconds!</green> Explore the area!</white>"),
                                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(5000), Duration.ofMillis(1000))
                        )) {
                    @Override
                    public AbstractAction determineNextAction(Player player, ActionList actionList) {
                        return actionList.getAction(0); // Next action in sequence
                    }
                },

                // Action 5: Team Aspect Dialog - A simple chat message to round off the tutorial
                new DialogAction(5, 6000, false, // Delay after previous title fades out
                        MiniMessage.miniMessage().deserialize("<gold>[NPC]</gold> " + name + "<white>: Remember to play with your team and have fun!</white>")) {
                    @Override
                    public AbstractAction determineNextAction(Player player, ActionList actionList) {
                        return actionList.getAction(0); // After the last line, jump back to action 0
                        // This allows the next click to restart the tutorial logic
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

        // Initialize a list to hold all packets to be sent.
        List<SendablePacket> packets = new ArrayList<>();

        // 1. Get the base NPC's spawn packets.
        // This includes SpawnEntityPacket, EntityMetaDataPacket for the NPC itself.
        // Assuming TemplatePlayerNPC has a method like getNpcSpawnPackets()
        packets.addAll(getNpcSpawnPackets(player));

        // 2. Get the name hologram's spawn packets.
        // This method specifically returns the packets required for the client to see the hologram.
        // Assuming TemplatePlayerNPC has a method like getNameSpawnPackets()
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

        // Get the base NPC's despawn packets.
        // Assuming TemplatePlayerNPC has a method like getNpcDespawnPackets()
        packets.addAll(getNpcDespawnPackets(player));

        // Get the name hologram's despawn packets.
        // This will retrieve the packets to destroy the hologram entities.
        // Assuming TemplatePlayerNPC has a method like getNameDespawnPackets()
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
     * It ensures the tutorial actions are triggered when the NPC is clicked.
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
