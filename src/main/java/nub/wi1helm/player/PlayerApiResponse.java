// nub.wi1helm.player/PlayerApiResponse.java
package nub.wi1helm.player;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;

// This class mirrors the JSON structure returned by your Go player-data-service
public class PlayerApiResponse {
    @SerializedName("UUID") // Match the Go JSON tags exactly
    private String uuid;

    @SerializedName("Username") // Match the Go JSON tags exactly
    private String username;

    @SerializedName("Team") // Match the Go JSON tags exactly
    private String team; // This will be "AQUA_CREEPERS" or "PURPLE_SWORDERS"

    @SerializedName("TotalPlaytimeTicks") // Match the Go JSON tags exactly
    private double totalPlaytimeTicks;

    @SerializedName("DeltaPlaytimeTicks") // Match the Go JSON tags exactly
    private double deltaPlaytimeTicks;

    @SerializedName("Banned") // Match the Go JSON tags exactly
    private boolean banned;

    @SerializedName("BanExpiresAt") // Match the Go JSON tags exactly
    private Instant banExpiresAt; // Use Instant for time.Time

    @SerializedName("LastLoginAt") // Match the Go JSON tags exactly
    private Instant lastLoginAt;

    @SerializedName("CreatedAt") // Match the Go JSON tags exactly
    private Instant createdAt;

    // Getters for all fields
    public String getUuid() { return uuid; }
    public String getUsername() { return username; }
    public String getTeam() { return team; }
    public double getTotalPlaytimeTicks() { return totalPlaytimeTicks; }
    public double getDeltaPlaytimeTicks() { return deltaPlaytimeTicks; }
    public boolean isBanned() { return banned; }
    public Instant getBanExpiresAt() { return banExpiresAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Instant getCreatedAt() { return createdAt; }

    // Keep these for backward compatibility if other code uses them
    public double getPlaytime() { return totalPlaytimeTicks; }
    public double getDeltaPlaytime() { return deltaPlaytimeTicks; }

    @Override
    public String toString() {
        return "PlayerApiResponse{" +
                "uuid='" + uuid + '\'' +
                ", username='" + username + '\'' +
                ", team='" + team + '\'' +
                ", totalPlaytimeTicks=" + totalPlaytimeTicks +
                ", deltaPlaytimeTicks=" + deltaPlaytimeTicks +
                ", banned=" + banned +
                ", banExpiresAt=" + banExpiresAt +
                ", lastLoginAt=" + lastLoginAt +
                ", createdAt=" + createdAt +
                '}';
    }
}