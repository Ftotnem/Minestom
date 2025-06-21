// ProfileDataComponent.java
package nub.wi1helm.data;

import nub.wi1helm.player.PlayerService;
import nub.wi1helm.server.DataLoadingComponent;
import nub.wi1helm.server.ServerProfile;

import java.util.concurrent.CompletableFuture;

public class ProfileDataComponent extends DataLoadingComponent<ServerProfile> {
    private final PlayerService playerService;

    public ProfileDataComponent() {
        super("Profile", true);
        this.playerService = PlayerService.getInstance();
    }

    @Override
    public CompletableFuture<ServerProfile> loadData(String uuid, String username) {
        return playerService.loadPlayerProfile(uuid, username);
    }
}