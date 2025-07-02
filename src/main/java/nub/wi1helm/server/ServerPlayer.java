package nub.wi1helm.server;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.coordinate.Pos;
import nub.wi1helm.Main;
import nub.wi1helm.content.time.ContentFactoryEntry;
import nub.wi1helm.game.GameHandler;
import nub.wi1helm.player.GameService;
import nub.wi1helm.player.PlayerDataManager;
import nub.wi1helm.content.time.TimeContent;
import nub.wi1helm.content.time.TimeContentFactory;
import nub.wi1helm.content.time.TimeContentManager;
import nub.wi1helm.template.npc.Nameable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static nub.wi1helm.Main.logger;

public class ServerPlayer extends Player {
    private ServerState currentState = ServerState.LOADING;
    private final PlayerDataManager dataManager;
    private final GameService gameService;
    private Pos intendedSpawnPoint;
    private final Map<String, TimeContent> playerTimeContents = new HashMap<>();

    // A set to store the identifiers of TimeContent views already sent.
    private final Set<String> receivedTimeContentViews = new HashSet<>();

    // A flag to track if initial spawn logic has been applied.
    private final AtomicBoolean initialSpawnLogicApplied = new AtomicBoolean(false);


    public ServerPlayer(@NotNull PlayerConnection playerConnection, @NotNull GameProfile gameProfile) {
        super(playerConnection, gameProfile);

        this.dataManager = new PlayerDataManager();
        this.gameService = GameService.getInstance();

        applyState(ServerState.LOADING);
        this.intendedSpawnPoint = ServerTeam.LIMBO.getPos();

        setupDataCallbacks();
        startDataLoading();


    }

    /**
     * Initializes this player's specific instances of TimeContent using registered factories.
     * This method should be called AFTER the player has been assigned to an Instance.
     */
    private void initializePlayerTimeContents() {
        if (this.getInstance() == null) {
            logger.error("Attempted to initialize TimeContent for player {} before instance was set.", getUsername());
            return;
        }
        if (!playerTimeContents.isEmpty()) {
            logger.debug("TimeContent already initialized for player {}. Skipping.", getUsername());
            return;
        }

        // Iterate through all content factories from the global manager, now with their identifiers
        for (ContentFactoryEntry entry : TimeContentManager.getInstance().getContentFactories()) {
            try {
                // Create a new instance of content for THIS PLAYER
                TimeContent content = entry.factory().create(Main.instance); // The factory creates the TimeContent object

                // Store it in the player's personal map, using the unique identifier as the key.
                // This ensures that even if two factories create objects of the same class,
                // they will have different keys if their registration identifiers are different.
                this.playerTimeContents.put(entry.identifier(), content);
                logger.debug("Created player-specific TimeContent: {} ({}) for {}",
                        content.getClass().getSimpleName(), entry.identifier(), getUsername());
            } catch (Exception e) {
                logger.error("Failed to create TimeContent from factory for {}: {}", getUsername(), e.getMessage(), e);
            }
        }
        logger.info("Initialized {} player-specific TimeContent instances for player {}", playerTimeContents.size(), getUsername());
    }


    private void setupDataCallbacks() {
        dataManager.setComponentCallback("Profile", (ServerProfile profile) -> {
            logger.info("Profile loaded for {}: team={}, banned={}, firstJoin={}",
                    getUsername(), profile.getServerTeam(), profile.isBanned(), profile.isFirstJoin());

            if (profile.isBanned()) {
                applyState(ServerState.BANNED);
            } else if (profile.isFirstJoin()) {
                applyState(ServerState.TEAM_SELECTION);
            } else {
                applyState(ServerState.PLAYING);
            }

            // IMPORTANT: Now that the profile is loaded, apply the initial spawn logic
            // This ensures display name and teleport are handled with loaded data.
            // Schedule on next tick to ensure Minestom's internal player add process is complete.
            scheduler().buildTask(this::applyInitialSpawnLogic).schedule();
        });
    }

    private void startDataLoading() {
        dataManager.loadRequiredData(getUuid().toString(), getUsername())
                .exceptionally(ex -> {
                    logger.error("Failed to load required data for {}: {}", getUsername(), ex.getMessage());
                    kick(Component.text("Failed to load your player data: " + ex.getMessage()));
                    return null;
                });
        dataManager.loadOptionalData(getUuid().toString(), getUsername());
    }

    private void applyState(ServerState newState) {
        if (currentState == newState) return;
        logger.debug("State change for {}: {} -> {}", getUsername(), currentState, newState);
        currentState = newState;
        setAutoViewable(newState.isVisible());

        switch (newState) {
            case LOADING:
            case TEAM_SELECTION:
            case BANNED:
                this.intendedSpawnPoint = ServerTeam.LIMBO.getPos();
                setRespawnPoint(ServerTeam.LIMBO.getPos()); // Keep respawn point consistent
                break;
            case PLAYING:
                if (getServerProfile() != null) {
                    this.intendedSpawnPoint = getServerProfile().getServerTeam().getPos();
                    setRespawnPoint(this.intendedSpawnPoint); // Set respawn point for playing state
                } else {
                    this.intendedSpawnPoint = ServerTeam.LIMBO.getPos(); // Fallback
                    setRespawnPoint(ServerTeam.LIMBO.getPos());
                }
                break;
        }
    }

    public void applyInitialSpawnLogic() {
        // Only run this logic once per full initial connection/data load.
        // Or after a major state transition like team selection.
        if (!initialSpawnLogicApplied.compareAndSet(false, true) && currentState == ServerState.LOADING) {
            // If already applied and not in a state that forces re-application (like PLAYING after team select),
            // then return to prevent redundant calls during loading phase.
            return;
        }

        if (!this.isOnline()) { // Check if player is still online
            logger.warn("Skipping applyInitialSpawnLogic for {} - Player is offline.", getUsername());
            return;
        }

        ServerProfile profile = getServerProfile();
        if (profile == null) {
            logger.error("ServerProfile is null during applyInitialSpawnLogic for {}. Kicking player.", getUsername());
            kick(Component.text("Failed to load your profile. Please reconnect."));
            return;
        }

        // Initialize player-specific TimeContent instances AFTER the instance is set
        if (this.getInstance() != null && playerTimeContents.isEmpty()) {
            initializePlayerTimeContents();
        }

        // --- Core Player Info Update Logic ---
        Component newDisplayName;
        PlayerSkin newSkin;
        ServerTeam team = profile.getServerTeam();

        if (profile.isBanned()) {
            newDisplayName = MiniMessage.miniMessage().deserialize("<red>" + profile.getTeamUsername() + "</red>");
            newSkin = team.getSkin(); // Even if banned, use their team's skin for consistency
        } else {
            newDisplayName = MiniMessage.miniMessage().deserialize("<" + team.color() + ">" + profile.getTeamUsername() + "</" + team.color() + ">");
            newSkin = team.getSkin();
        }

        // 1. Update player's own display name (for above-head display)
        setDisplayName(newDisplayName);

        // 2. Update player's own skin
        setSkin(newSkin);

        // 3. Send PlayerInfoUpdatePacket to all players for tab list refresh
        // This is crucial for other players to see the updated name and skin in the tab list.
        updatePlayerInfoInTabList(newDisplayName, newSkin);


        // --- Teleport and Game Mode Logic ---
        switch (currentState) {
            case LOADING: // Should ideally not be here if profile loaded
                teleport(ServerTeam.LIMBO.getPos());
                setGameMode(ServerState.LOADING.getGameMode());
                sendMessage(Component.text("Your player data is still loading... Please wait."));
                break;

            case TEAM_SELECTION:
                teleport(ServerTeam.LIMBO.getPos());
                setGameMode(ServerState.TEAM_SELECTION.getGameMode());
                sendMessage(Component.text("Welcome, new player! Selecting your team."));
                GameHandler.playTeamSelectAnimation(this, team);
                break;

            case PLAYING:
                teleport(intendedSpawnPoint);
                setGameMode(ServerState.PLAYING.getGameMode());
                // Respawn point already set in applyState, ensures consistency after death
                final Title title = Title.title(MiniMessage.miniMessage().deserialize("<" + getServerTeam().color() + ">Welcome back, " + getServerProfile().getTeamUsername() + "</" + getServerTeam().color() + ">"), Component.text(""));
                showTitle(title);
                break;

            case BANNED:
                teleport(ServerTeam.LIMBO.getPos());
                setGameMode(ServerState.BANNED.getGameMode());
                sendMessage(Component.text("You are banned from this server."));
                break;
        }
        // Always clear and potentially re-send views on initial spawn logic to ensure consistency
        clearReceivedTimeContentViews();
    }

    /**
     * Sends a PlayerInfoUpdatePacket to all online players to update this player's
     * display name and skin in their tab lists.
     * This is a critical step to ensure consistent visibility.
     */
    private void updatePlayerInfoInTabList(Component displayName, PlayerSkin skin) {
        // Create an entry with updated display name and skin
        PlayerInfoUpdatePacket.Entry entry = new PlayerInfoUpdatePacket.Entry(
                getUuid(),                                    // Player UUID
                getServerProfile().getTeamUsername(),                                // Minestom requires actual username here for GameProfile.name
                skin != null ? List.of(new PlayerInfoUpdatePacket.Property("textures", skin.textures(), skin.signature())) : List.of(), // Skin properties
                true,                                         // Listed in tab list
                getLatency(),                                 // Player latency
                getGameMode(),                                // Player game mode
                displayName,                                  // The actual display name component!
                null,                                         // Chat session (optional, typically null)
                0                                             // Deaths (optional)
        );

        // Send the UPDATE_DISPLAY_NAME action to all players.
        // It's safer to send UPDATE_DISPLAY_NAME and UPDATE_SKIN/PROPERTIES separately
        // if Minestom supported them as distinct actions. Since it bundles them,
        // we send a combined UPDATE_DISPLAY_NAME.
        PlayerInfoUpdatePacket updatePacket = new PlayerInfoUpdatePacket(
                EnumSet.of(PlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), // Action to update display name
                List.of(entry)
        );

        // Also update the skin if it changed. Minestom typically uses UPDATE_GAMEMODE
        // to force a refresh on appearance if setSkin is called.
        // The most robust way is to update ALL_PLAYER_INFO_PROPERTIES if needed.
        // For simplicity and common practice, UPDATE_DISPLAY_NAME should also carry skin updates
        // if the entry is re-sent.
        PlayerInfoUpdatePacket skinUpdatePacket = new PlayerInfoUpdatePacket(
                EnumSet.of(PlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, PlayerInfoUpdatePacket.Action.UPDATE_LISTED, PlayerInfoUpdatePacket.Action.UPDATE_LATENCY),
                List.of(entry) // Re-use the same entry with all updated data
        );


        // Send this packet to *all* currently online players, including self,
        // so everyone's tab list is consistent.
        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player -> {
            player.sendPacket(updatePacket);
            // player.sendPacket(skinUpdatePacket); // Might be redundant or cause flicker, test this.
        });
        // Send to self also to ensure local client's tab list is correct.
        sendPacket(updatePacket);
        // sendPacket(skinUpdatePacket); // If you send to others, send to self too.

        logger.debug("Sent PlayerInfoUpdatePacket for {} with display name: {}", getUsername(), displayName.style().color());
    }


    public void completeTeamSelection() {
        if (getServerProfile() != null) {
            getServerProfile().setFirstJoin(false);
        }
        applyState(ServerState.PLAYING); // This updates intendedSpawnPoint
        initialSpawnLogicApplied.set(false); // Allow applyInitialSpawnLogic to re-run for a new state

        scheduler().buildTask(() -> {
            if (this.isOnline()) {
                applyInitialSpawnLogic(); // Re-apply spawn logic to teleport and send content
            }
        }).schedule();
    }

    @Override
    public void tick(long time) {
        super.tick(time);
        if (this.getInstance() != null) {
            updateActionBar();
        }

        if (currentState.allowTimeUpdates()) {
            updatePlaytimeData();
            updateTimeContent();
        }
    }

    private void updatePlaytimeData() {
        String uuid = getUuid().toString();
        gameService.getPlayerTotalPlaytime(uuid)
                .thenAccept(latestPlaytime -> {
                    if (getServerProfile() != null) {
                        getServerProfile().setPlaytime(latestPlaytime);
                    }
                })
                .exceptionally(e -> {
                    logger.warn("Failed to fetch latest total playtime for {}: {}", uuid, e.getMessage());
                    return null;
                });

        gameService.getPlayerDeltaPlaytime(uuid)
                .thenAccept(latestDeltaPlaytime -> {
                    if (getServerProfile() != null) {
                        getServerProfile().setDeltaPlaytime(latestDeltaPlaytime);
                    }
                })
                .exceptionally(e -> {
                    logger.warn("Failed to fetch latest delta playtime for {}: {}", uuid, e.getMessage());
                    return null;
                });
    }

    private void updateActionBar() {
        sendActionBar(currentState.getActionBarComponent(this));
    }

    /**
     * Checks if the player's delta playtime has surpassed thresholds for any TimeContent
     * they haven't yet received a view for, and sends those packets.
     * This now uses the player's *own* content instances.
     */
    private void updateTimeContent() {
        // Ensure content is initialized before attempting to update it
        if (playerTimeContents.isEmpty() && this.getInstance() != null) {
            initializePlayerTimeContents();
            if (playerTimeContents.isEmpty()) {
                return;
            }
        }

        // Iterate over the entries to get both the unique key and the content object
        for (Map.Entry<String, TimeContent> entry : playerTimeContents.entrySet()) {
            String uniqueContentKey = entry.getKey(); // This is your unique identifier (e.g., "daily_challenge_sign_1")
            TimeContent content = entry.getValue();

            if (content.canSpawn(this) && !receivedTimeContentViews.contains(uniqueContentKey)) {
                Collection<SendablePacket> packets = content.getSpawnPackets(this);

                if (packets != null && !packets.isEmpty()) {
                    Main.logger.debug("Sending packets for Player-specific TimeContent: {} - {} packets to {}",
                            uniqueContentKey, packets.size(), getUsername());
                    sendPackets(packets);
                    addReceivedTimeContentView(uniqueContentKey); // Store the unique key
                }
            }
        }
    }

    // Public getters
    public Pos getIntendedSpawnPoint() { return intendedSpawnPoint; }
    public ServerState getCurrentState() { return currentState; }
    public ServerProfile getServerProfile() { return dataManager.getProfile(); }
    public boolean isDataComponentLoaded(String componentName) { return dataManager.isComponentLoaded(componentName); }
    public <T> T getDataComponent(String componentName, Class<T> type) { return dataManager.getComponentData(componentName, type); }
    public double getCurrentPlaytime() { ServerProfile profile = getServerProfile(); return profile != null ? profile.getCurrentPlaytime() : 0.0; }
    public double getDeltaPlaytime() { ServerProfile profile = getServerProfile(); return profile != null ? profile.getDeltaPlaytime() : 1.0; }
    public boolean isBanned() { ServerProfile profile = getServerProfile(); return profile != null && profile.isBanned(); }
    public ServerTeam getServerTeam() { ServerProfile profile = getServerProfile(); return profile != null ? profile.getServerTeam() : null; }

    /**
     * Provides access to this player's specific TimeContent instances.
     * This is used by listeners (like PlayerDiggingListener) to check per-player content.
     * @return A map of unique content IDs (class names) to TimeContent instances.
     */
    public Map<String, TimeContent> getPlayerTimeContents() {
        return playerTimeContents;
    }

    public void addReceivedTimeContentView(String uniqueContentKey) { // Accept the unique key
        receivedTimeContentViews.add(uniqueContentKey);
    }

    @Override
    protected @NotNull PlayerInfoUpdatePacket getAddPlayerToList() {
        // This is called by Minestom *very early* in the connection process.
        // The ServerProfile might not be loaded yet.
        // We provide default/placeholder values here, and then update once data is loaded.
        ServerProfile profile = getServerProfile();
        Component tabDisplayName;
        PlayerSkin skin;

        if (profile != null) {
            // Data is loaded, use actual values
            tabDisplayName = MiniMessage.miniMessage().deserialize("<" + profile.getServerTeam().color() + ">" + profile.getTeamUsername() + "</" + profile.getServerTeam().color() + ">");
            skin = profile.getServerTeam().getSkin();
        } else {
            // Data not loaded yet, use placeholders or real username for initial packet
            tabDisplayName = Component.text(getUsername()); // Use actual Minecraft username temporarily
            skin = getSkin(); // Use default skin if no team skin loaded yet
        }

        List<PlayerInfoUpdatePacket.Property> properties = skin != null ?
                List.of(new PlayerInfoUpdatePacket.Property("textures", skin.textures(), skin.signature())) :
                List.of();

        PlayerInfoUpdatePacket.Entry entry = new PlayerInfoUpdatePacket.Entry(
                getUuid(),
                getServerProfile().getTeamUsername(), // Always use the *actual* Minecraft username for GameProfile.name in the entry
                properties,
                true, // Player should be listed by default
                getLatency(),
                getGameMode(),
                tabDisplayName, // This is the component displayed in the tab list
                null, 0
        );

        // Minestom's internal logic will use this to ADD_PLAYER.
        return new PlayerInfoUpdatePacket(EnumSet.of(PlayerInfoUpdatePacket.Action.ADD_PLAYER, PlayerInfoUpdatePacket.Action.UPDATE_LISTED),
                List.of(entry));
    }
    public void clearReceivedTimeContentViews() {
        // First, collect all despawn packets from currently managed content
        Collection<SendablePacket> despawnPackets = playerTimeContents.values().stream()
                .flatMap(c -> c.getDespawnPackets(this).stream())
                .collect(Collectors.toList());

        // Send despawn packets to the player if there are any
        if (!despawnPackets.isEmpty()) {
            sendPackets(despawnPackets);
            logger.debug("Sent {} despawn packets for {}'s TimeContent.", despawnPackets.size(), getUsername());
        }

        // Then, clear the set of views
        receivedTimeContentViews.clear();
        logger.debug("Cleared receivedTimeContentViews for {}.", getUsername());
    }

    /**
     * Gets a specific TimeContent instance for this player by its unique identifier (the key you registered it with).
     * @param identifier The unique string identifier used during registration in TimeContentManager.
     * @param <T> The type of TimeContent.
     * @return The player's specific instance of the TimeContent, or null if not found.
     */
    public <T extends TimeContent> T getPlayerTimeContent(String identifier) {
        return (T) playerTimeContents.get(identifier);
    }
}