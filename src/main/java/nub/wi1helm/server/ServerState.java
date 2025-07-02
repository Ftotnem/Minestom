package nub.wi1helm.server;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.entity.GameMode;

public enum ServerState {
    LOADING(GameMode.ADVENTURE, false),
    TEAM_SELECTION(GameMode.ADVENTURE, true),
    PLAYING(GameMode.SURVIVAL, true),
    BANNED(GameMode.ADVENTURE, false);

    private final GameMode gameMode;
    private final boolean visible;

    ServerState(GameMode gameMode, boolean visible) {
        this.gameMode = gameMode;
        this.visible = visible;
    }

    public GameMode getGameMode() { return gameMode; }
    public boolean isVisible() { return visible; }

    public Component getActionBarComponent(ServerPlayer player) {
        ServerProfile profile = player.getServerProfile(); // Get profile once

        switch (this) {
            case LOADING:
                return MiniMessage.miniMessage().deserialize("<gold>Loading your data...</gold>");
            case TEAM_SELECTION:
                return MiniMessage.miniMessage().deserialize("<aqua>Choose your team!</aqua>");
            case PLAYING:
                if (profile != null) {
                    long currentPlaytimeSeconds = (long) (profile.getCurrentPlaytime() / 20.0);
                    double deltaPlaytimeSeconds = profile.getDeltaPlaytime();

                    String teamUsername = profile.getTeamUsername();
                    String teamColor = profile.getServerTeam().color().asHexString(); // Get the team's color string

                    return MiniMessage.miniMessage().deserialize(
                            "<" + teamColor + ">" + teamUsername + "</" + teamColor + "> | " +
                                    "<gray>" + formatSecondsWithDots(currentPlaytimeSeconds) +  " s</gray> | " +
                                    "<dark_gray>+" + String.format("%.1f", deltaPlaytimeSeconds) + " s/s</dark_gray>"
                    );
                }
                return Component.text("Playing..."); // Fallback if profile is null
            case BANNED:
                // For a banned player, we still want to show their stats if available.
                // Default to player's actual username and a default color if profile isn't loaded.
                String bannedTeamUsername = (profile != null) ? profile.getTeamUsername() : player.getUsername();
                // Default to a dark_red color for banned users if their team color isn't available
                String bannedTeamColor = (profile != null && profile.getServerTeam() != null) ? profile.getServerTeam().color().asHexString() : "#880000";

                long bannedCurrentPlaytimeSeconds = (profile != null) ? (long) (profile.getCurrentPlaytime() / 20.0) : 0;
                // For a banned player, delta playtime is effectively 0 as they are not gaining time.
                // We'll display 0.0 s/s with strikethrough.
                double bannedDeltaPlaytimeSeconds = 0.0;

                return MiniMessage.miniMessage().deserialize(
                        "<red>BANNED!</red> | " + // Clear ban indicator in red
                                "<" + bannedTeamColor + ">" + bannedTeamUsername + "</" + bannedTeamColor + "> | " +
                                "<gray>Time: " + formatSecondsWithDots(bannedCurrentPlaytimeSeconds) + "</gray> | " +
                                // Strikethrough the delta to show it's inactive
                                "<dark_gray><strikethrough>+" + String.format("%.1f", bannedDeltaPlaytimeSeconds) + " s/s</strikethrough></dark_gray>"
                );
            default:
                return Component.empty();
        }
    }

    /**
     * Formats a total number of seconds into the "XX.XXX.XXX" format.
     * The number is zero-padded to 8 digits, and dots are inserted.
     * This method is optimized for a fixed display length where the maximum
     * value (10,000,000 seconds) fits within 8 digits.
     *
     * Examples:
     * 0       -> "00.000.000"
     * 1       -> "00.000.001"
     * 1234    -> "00.001.234"
     * 1000000 -> "01.000.000"
     * 10000000-> "10.000.000"
     *
     * @param totalSeconds The total number of seconds.
     * @return The formatted string.
     */
    private String formatSecondsWithDots(long totalSeconds) {
        String paddedSeconds = String.format("%08d", totalSeconds);
        StringBuilder sb = new StringBuilder(paddedSeconds);
        sb.insert(2, '.');
        sb.insert(6, '.');
        return sb.toString();
    }

    public boolean allowTimeUpdates() {
        return this == PLAYING;
    }
}