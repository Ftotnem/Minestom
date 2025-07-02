package nub.wi1helm.content.npc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
import nub.wi1helm.template.npc.actions.TitleAction;
import nub.wi1helm.template.npc.player.SkinLayer;
import nub.wi1helm.template.npc.player.TemplatePlayerNPC;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DailyEventNPC extends TemplatePlayerNPC implements TimeContent, Nameable, Interactable {

    private Pos localPos;
    private double time;
    private ActionList actionList = ActionList.empty(); // Initialize with an empty list

    /**
     * Constructs a new DailyEventNPC.
     *
     * @param instance The instance the NPC will be spawned in.
     * @param pos The local position of the NPC within its designated area.
     * @param time The game time at which this NPC should appear.
     */
    public DailyEventNPC(Instance instance, Pos pos, double time) {
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
     * Personalizes the NPC for a specific player.
     * This includes setting the NPC's skin to match the player's and defining the
     * sequence of Title-based actions for the daily event explanation.
     *
     * @param player The player for whom to personalize the NPC.
     */
    @Override
    public void personalize(Player player) {
        final ServerPlayer p = (ServerPlayer) player;
        setSkinLayer(SkinLayer.FULL);
        setSkin(p.getSkin()); // Set NPC's skin to match the player's

        String name = "Event Teller"; // Default name for the NPC
        // Adjust name based on player's team for personalization
        if (p.getServerTeam() == ServerTeam.AQUA_CREEPERS) {
            name = "<" + ServerTeam.AQUA_CREEPERS.color() + ">Creeper Elder</" + ServerTeam.AQUA_CREEPERS.color() + ">";
        }
        if (p.getServerTeam() == ServerTeam.PURPLE_AXOLOTLS) {
            name = "<" + ServerTeam.PURPLE_AXOLOTLS.color() + ">Axolotl Oracle</" + ServerTeam.PURPLE_AXOLOTLS.color() + ">";
        }

        // Set the NPC's name hologram above its head.
        setName(Main.instance, MiniMessage.miniMessage().deserialize("<dark_purple>[EVENT]</dark_purple> " + name), MiniMessage.miniMessage().deserialize("<yellow><bold>DAILY CHALLENGE!</bold></yellow>"));

        // Define the sequence of TitleActions for the daily event tutorial
        setActionList(new ActionList(
                // Action 0: Entry point for ANY click on this NPC
                new AbstractAction(0, 0, true) {
                    @Override
                    public void execute(Player player) {
                        // No immediate output needed for the entry point
                    }

                    @Override
                    public AbstractAction determineNextAction(Player player, ActionList actionList) {
                        return actionList.getAction(1); // Proceed to the first title message
                    }
                },

                // Action 1: Introduction to the Daily Event
                new TitleAction(1, 0, false,
                        Title.title(
                                MiniMessage.miniMessage().deserialize("<dark_aqua>The Daily Color Challenge!</dark_aqua>"),
                                MiniMessage.miniMessage().deserialize("<white>A new mystery awaits you!</white>"),
                                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))
                        )) {
                    @Override
                    public AbstractAction determineNextAction(Player player, ActionList actionList) {
                        return actionList.getAction(2); // Next action in sequence
                    }
                },

                // Action 2: Explanation of Choice
                new TitleAction(2, 4000, false, // Delay after previous action completes
                        Title.title(
                                MiniMessage.miniMessage().deserialize("<gold>Choose Your Color!</gold>"),
                                MiniMessage.miniMessage().deserialize("<white>Select one of <green>four mysterious colors</green>.</white>"),
                                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(4000), Duration.ofMillis(1000))
                        )) {
                    @Override
                    public AbstractAction determineNextAction(Player player, ActionList actionList) {
                        return actionList.getAction(3); // Next action in sequence
                    }
                },

                // Action 3: The Daily Reveal
                new TitleAction(3, 5000, false, // Delay after previous action completes
                        Title.title(
                                MiniMessage.miniMessage().deserialize("<light_purple>The True Color Revealed!</light_purple>"),
                                MiniMessage.miniMessage().deserialize("<white>At day's end, one color is <aqua>correct</aqua>!</white>"),
                                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(4000), Duration.ofMillis(1000))
                        )) {
                    @Override
                    public AbstractAction determineNextAction(Player player, ActionList actionList) {
                        return actionList.getAction(4); // Next action in sequence
                    }
                },

                // Action 4: Reward for Correct Choice
                new TitleAction(4, 5000, false, // Delay after previous action completes
                        Title.title(
                                MiniMessage.miniMessage().deserialize("<green>Victory Boost!</green>"),
                                MiniMessage.miniMessage().deserialize("<white>Choose correctly for a special <yellow>playtime boost</yellow>!</white>"),
                                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(5000), Duration.ofMillis(1000))
                        )) {
                    @Override
                    public AbstractAction determineNextAction(Player player, ActionList actionList) {
                        return actionList.getAction(5); // Next action in sequence
                    }
                },

                // Action 5: No Penalty for Incorrect Choice
                new TitleAction(5, 6000, false, // Delay after previous action completes
                        Title.title(
                                MiniMessage.miniMessage().deserialize("<red>No Luck?</red>"),
                                MiniMessage.miniMessage().deserialize("<gray>Don't worry! No penalty for incorrect guesses.</gray>"),
                                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(4000), Duration.ofMillis(1000))
                        )) {
                    @Override
                    public AbstractAction determineNextAction(Player player, ActionList actionList) {
                        return actionList.getAction(6); // Next action in sequence
                    }
                },

                // Action 6: Call to Action - Check back daily
                new TitleAction(6, 5000, false, // Delay after previous action completes
                        Title.title(
                                MiniMessage.miniMessage().deserialize("<blue>Participate Daily!</blue>"),
                                MiniMessage.miniMessage().deserialize("<white>Check back tomorrow for a new challenge!</white>"),
                                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(5000), Duration.ofMillis(1000))
                        )) {
                    @Override
                    public AbstractAction determineNextAction(Player player, ActionList actionList) {
                        return actionList.getAction(0); // Loop back to the start for next interaction
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
        // Assumes TemplatePlayerNPC has a protected method getNpcSpawnPackets()
        packets.addAll(getNpcSpawnPackets(player));

        // 2. Get the name hologram's spawn packets.
        // This method specifically returns the packets required for the client to see the hologram.
        // Assumes TemplatePlayerNPC has a protected method getNameSpawnPackets()
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
        // Assumes TemplatePlayerNPC has a protected method getNpcDespawnPackets()
        packets.addAll(getNpcDespawnPackets(player));

        // Get the name hologram's despawn packets.
        // This will retrieve the packets to destroy the hologram entities.
        // Assumes TemplatePlayerNPC has a protected method getNameDespawnPackets()
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
     * It ensures the daily event explanation actions are triggered when the NPC is clicked.
     */
    public void interact() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerEntityInteractEvent.class, event -> {
            // Ignore off-hand interactions
            if (event.getHand().equals(PlayerHand.OFF)) return;
            // Check if the interacted entity is this NPC
            if (event.getTarget().getUuid().equals(this.getUuid())) {
                onInteract(event.getPlayer()); // Trigger the interaction logic defined in the ActionList
            }
        });
    }
}
