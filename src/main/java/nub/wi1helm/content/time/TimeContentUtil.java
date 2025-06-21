package nub.wi1helm.content.time;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import nub.wi1helm.server.ServerPlayer;
import nub.wi1helm.server.ServerTeam;


public class TimeContentUtil {
    public static Point transformToAbsolute(Point local, ServerTeam team) {
        return switch (team) {
            case AQUA_CREEPERS -> local.mul(1, 1, -1).add(team.getPos());
            case PURPLE_AXOLOTLS -> local.mul(-1, 1, 1).add(team.getPos());
            default -> Pos.ZERO;
        };
    }

    /**
     * Checks if the player's current playtime meets or exceeds the required content time.
     *
     * @param playerPlaytime The current playtime of the player in ticks.
     * @param contentTime The time at which the content should appear.
     * @return True if the content can appear, false otherwise.
     */
    public static boolean checkTime(double playerPlaytime, double contentTime) {
        return (playerPlaytime / 20) >= contentTime;
    }
}