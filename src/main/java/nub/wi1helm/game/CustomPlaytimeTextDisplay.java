package nub.wi1helm.game;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import nub.wi1helm.server.ServerTeam;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// This class will need access to the latestTeamPlaytimes map from GameHandler.
// We'll make latestTeamPlaytimes public static in GameHandler for this specific cross-class access.
// Or, a better approach: Pass the map as a dependency or have a getter in GameHandler.
// For simplicity and given the static nature of GameHandler, we'll keep it public static for now.

public class CustomPlaytimeTextDisplay extends Entity {

    private final ServerTeam team;

    // We'll assume GameHandler exposes this map for access
    // This isn't ideal for strict encapsulation, but practical for static GameHandler.
    // A more robust solution might involve passing a function/supplier.
    private static final Map<ServerTeam, Double> latestTeamPlaytimes = GameHandler.getLatestTeamPlaytimes();

    public CustomPlaytimeTextDisplay(ServerTeam team) {
        super(EntityType.TEXT_DISPLAY);
        this.team = team;
        // Apply static metadata common to all these displays
        this.editEntityMeta(TextDisplayMeta.class, textDisplayMeta -> {
            textDisplayMeta.setHasNoGravity(true);
            textDisplayMeta.setScale(new Vec(10, 10, 10));
            textDisplayMeta.setViewRange(200);
            textDisplayMeta.setBackgroundColor(16711680); // Red color for background, adjust as needed
        });
    }

    @Override
    public void tick(long time) {
        super.tick(time); // Call super tick to maintain default entity behavior

        // Read from the local cache, which is updated periodically by GameHandler's scheduler
        double totalPlaytime = latestTeamPlaytimes.getOrDefault(team, 0.0);

        this.editEntityMeta(TextDisplayMeta.class, textDisplayMeta -> {
            // Convert ticks to seconds, then format
            long playtimeSeconds = (long) (totalPlaytime / 20.0);
            String formattedTime = String.format("%08d", playtimeSeconds);
            String displayTime = formattedTime.replaceAll("(?<=\\d)(?=(\\d{3})+$)", ".");

            textDisplayMeta.setText(MiniMessage.miniMessage().deserialize(
                    "<" + team.color() + "><bold>" +
                            team.displayName() + "</bold><br>" + // Display team name bold
                            displayTime + "/10.000.000</" + team.color() + ">" // Display playtime, end color
            ));
        });
    }
}