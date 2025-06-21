// nub.wi1helm.player/PlayerApiResponse.java
package nub.wi1helm.player;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;

// This class mirrors the JSON structure returned by your Go player-data-service
public class PlayerApiResponse {
    @SerializedName("uuid") // Match the Go JSON tags exactly
    private String uuid;

    @SerializedName("username") // Match the Go JSON tags exactly
    private String username;

    @SerializedName("team") // Match the Go JSON tags exactly
    private String team; // This will be "AQUA_CREEPERS" or "PURPLE_AXOLOTLS."

    @SerializedName("team_username")
    private String teamUsername;

    @SerializedName("current_playtime") // Match the Go JSON tags exactly
    private double currentPlaytime;

    @SerializedName("delta_playtime") // Match the Go JSON tags exactly
    private double deltaPlaytime;

    @SerializedName("banned") // Match the Go JSON tags exactly
    private boolean banned;

    @SerializedName("ban_expires_at") // Match the Go JSON tags exactly
    private Instant banExpiresAt; // Use Instant for time.Time

    @SerializedName("last_login_at") // Match the Go JSON tags exactly
    private Instant lastLoginAt;

    @SerializedName("created_at") // Match the Go JSON tags exactly
    private Instant createdAt;

    // Getters for all fields
    public String getUuid() { return uuid; }
    public String getUsername() { return username; }
    public String getTeam() { return team; }
    public boolean isBanned() { return banned; }
    public Instant getBanExpiresAt() { return banExpiresAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Instant getCreatedAt() { return createdAt; }
    public double getCurrentPlaytime() { return currentPlaytime; }
    public double getDeltaPlaytime() { return deltaPlaytime; }
    public String getTeamUsername() { return teamUsername;}

    @Override
    public String toString() {
        return "PlayerApiResponse{" +
                "uuid='" + uuid + '\'' +
                ", username='" + username + '\'' +
                ", team='" + team + '\'' +
                ", teamUsername='" + teamUsername + '\'' +
                ", currentPlaytime=" + currentPlaytime +
                ", deltaPlaytime=" + deltaPlaytime +
                ", banned=" + banned +
                ", banExpiresAt=" + banExpiresAt +
                ", lastLoginAt=" + lastLoginAt +
                ", createdAt=" + createdAt +
                '}';
    }
}