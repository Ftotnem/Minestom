package nub.wi1helm.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer; // Still needed for connection manager
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event; // New import for EventNode
import net.minestom.server.event.EventFilter; // New import for EventNode
import net.minestom.server.event.EventNode; // New import for EventNode
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.trait.PlayerEvent; // New import for PlayerEvent
import nub.wi1helm.server.ServerPlayer; // Assuming ServerPlayer is in this package

import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;

public class ChatHandler {

    // Define the minimum playtime required to chat in seconds
    private static final int MIN_CHAT_PLAYTIME_SECONDS = 200;
    // Minecraft ticks per second
    private static final int TICKS_PER_SECOND = 20;

    // Singleton instance
    static ChatHandler instance;

    // EventNode for this handler
    EventNode<PlayerEvent> node = EventNode.type("chat", EventFilter.PLAYER);

    // Private constructor for the Singleton pattern
    private ChatHandler(@NotNull EventNode<Event> global) {
        // Add this handler's node as a child to the global event node
        global.addChild(this.node);
        setupListeners();
    }

    /**
     * Gets the singleton instance of ChatHandler, initializing it if necessary.
     * This method should be called once with the global EventNode during plugin setup.
     *
     * @param global The global EventNode to which this handler's node will be attached.
     * @return The singleton instance of ChatHandler.
     */
    @NotNull
    public static ChatHandler getInstance(@NotNull EventNode<Event> global) {
        if (instance == null) {
            instance = new ChatHandler(global);
        }
        return instance;
    }

    @Nullable
    public static ChatHandler getInstance() {
        return instance;
    }

    // Renamed 'setup' to 'setupListeners' and made it private,
    // as it's now called from the constructor.
    private void setupListeners() {
        // Listener for player chat messages
        node.addListener(PlayerChatEvent.class, event -> {
            Player player = event.getPlayer();
            // Ensure the player is an instance of ServerPlayer
            if (player instanceof ServerPlayer sender) {
                // Check if the player has enough playtime to chat
                double playtimeTicks = sender.getCurrentPlaytime();
                if ((playtimeTicks / TICKS_PER_SECOND) < MIN_CHAT_PLAYTIME_SECONDS) {
                    // If playtime is insufficient, send a message and cancel the chat event
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You need at least " + MIN_CHAT_PLAYTIME_SECONDS + " seconds of playtime to chat!</red>"));
                    event.setCancelled(true);
                    return; // Stop further processing for this event
                }

                String message = event.getRawMessage();
                event.setCancelled(true); // Cancel the default chat message broadcast

                // Iterate through all online players to send them the formatted message
                MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(receiver -> {
                    // Ensure the receiver is also an instance of ServerPlayer
                    if (receiver instanceof ServerPlayer serverReceiver) {
                        // Format the message with sender's username, colored by their team
                        String formattedMessage = String.format(
                                "<%s>%s</%s>: <gray>%s</gray>", // Added color tags around the username
                                sender.getServerTeam().color(), // Get team color for opening tag
                                sender.getServerProfile().getTeamUsername(),
                                sender.getServerTeam().color(), // Get team color for closing tag
                                message
                        );
                        // Send the customized message using MiniMessage directly
                        receiver.sendMessage(MiniMessage.miniMessage().deserialize(formattedMessage));
                    }
                });
            }
        });

        // Listener for player spawn events (joining the server or switching worlds)
        node.addListener(PlayerSpawnEvent.class, event -> {
            Player joiningPlayer = event.getPlayer();
            // Ensure the joining player is an instance of ServerPlayer
            if (joiningPlayer instanceof ServerPlayer sender) {
                // Iterate through all online players to send them the join message
                MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(receiver -> {
                    // Do not send the message to the joining player themselves
                    if (receiver != joiningPlayer && receiver instanceof ServerPlayer serverReceiver) {
                        // Format the join message with joining player's username, colored by their team
                        String formattedMessage = String.format(
                                "<dark_gray>[<green>+</green>]</dark_gray> <%s>%s</%s>", // Added color tags around the username
                                sender.getServerTeam().color(), // Get team color for opening tag
                                sender.getServerProfile().getTeamUsername(),
                                sender.getServerTeam().color() // Get team color for closing tag
                        );
                        // Send the customized message using MiniMessage directly
                        receiver.sendMessage(MiniMessage.miniMessage().deserialize(formattedMessage));
                    }
                });
            }
        });

        // Listener for player disconnect events
        node.addListener(PlayerDisconnectEvent.class, event -> {
            Player leavingPlayer = event.getPlayer();
            // Ensure the leaving player is an instance of ServerPlayer
            if (leavingPlayer instanceof ServerPlayer sender) {
                // Iterate through all online players to send them the leave message
                MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(receiver -> {
                    // Do not send the message to the leaving player themselves (they are already disconnecting)
                    if (receiver != leavingPlayer && receiver instanceof ServerPlayer serverReceiver) {
                        // Format the leave message with leaving player's username, colored by their team
                        String formattedMessage = String.format(
                                "<dark_gray>[<red>-</red>]</dark_gray> <%s>%s</%s>", // Added color tags around the username
                                sender.getServerTeam().color(), // Get team color for opening tag
                                sender.getServerProfile().getTeamUsername(),
                                sender.getServerTeam().color() // Get team color for closing tag
                        );
                        // Send the customized message using MiniMessage directly
                        receiver.sendMessage(MiniMessage.miniMessage().deserialize(formattedMessage));
                    }
                });
            }
        });
    }
}
