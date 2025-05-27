// nub.wi1helm.server/ServerProfile.java
package nub.wi1helm.server;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public class ServerProfile {
    private final String uuid;
    private final String username;
    private double playtime;
    private double deltaPlaytime;
    private boolean banned;
    private @Nullable Instant banExpiresAt; // New field for ban expiration
    private ServerTeam serverTeam;
    private final @Nullable Instant lastLoginAt;
    private final @Nullable Instant createdAt;
    private boolean isFirstJoin; // New field for first join status

    public ServerProfile(
            @NotNull String uuid,
            @NotNull String username,
            double playtime,
            double deltaPlaytime,
            boolean banned,
            @Nullable Instant banExpiresAt, // Added to constructor
            @NotNull ServerTeam serverTeam,
            @Nullable Instant lastLoginAt,
            @Nullable Instant createdAt) {
        this.uuid = uuid;
        this.username = username;
        this.playtime = playtime;
        this.deltaPlaytime = deltaPlaytime;
        this.banned = banned;
        this.banExpiresAt = banExpiresAt; // Assign
        this.serverTeam = serverTeam;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
        this.isFirstJoin = false; // Default to false, will be set by PlayerService
    }

    // --- Getters ---
    public @NotNull String getUuid() { return uuid; }
    public @NotNull String getUsername() { return username; }
    public double getPlaytime() { return playtime; }
    public double getDeltaPlaytime() { return deltaPlaytime; }
    public boolean isBanned() { return banned; }
    public @Nullable Instant getBanExpiresAt() { return banExpiresAt; } // Getter for new field
    public @NotNull ServerTeam getServerTeam() { return serverTeam; }
    public @Nullable Instant getLastLoginAt() { return lastLoginAt; }
    public @Nullable Instant getCreatedAt() { return createdAt; }
    public boolean isFirstJoin() { return isFirstJoin; } // Getter for new field

    // --- Setters (for fields that might change post-load if needed, or by internal logic) ---
    public void setPlaytime(double playtime) { this.playtime = playtime; }
    public void setDeltaPlaytime(double deltaPlaytime) { this.deltaPlaytime = deltaPlaytime; }
    public void setBanned(boolean banned) { this.banned = banned; }
    public void setBanExpiresAt(@Nullable Instant banExpiresAt) { this.banExpiresAt = banExpiresAt; }
    public void setServerTeam(@NotNull ServerTeam serverTeam) { this.serverTeam = serverTeam; }
    public void setFirstJoin(boolean firstJoin) { this.isFirstJoin = firstJoin; } // Setter for new field
}