package nub.wi1helm.content.time;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import nub.wi1helm.server.ServerPlayer;
import nub.wi1helm.server.ServerTeam;


public class TimeContentUtil {
    public static Point transformToAbsolute(TimeContent content, ServerTeam team) {
        return switch (team) {
            case AQUA_CREEPERS -> {
                yield content.getLocalPos().mul(1, 1, 1).add(team.getPos());
            }
            case PURPLE_AXOLOTLS -> {
                // This is the key change:
                if (content instanceof TimeBlock block) {

                    yield block.getLocalPos().mul(-1, 1, -1).add(team.getPos()).add(new Pos(-1,0,-1));
                }

                yield content.getLocalPos().mul(-1, 1, -1).add(team.getPos());
            }
            default -> Pos.ZERO; // Assuming Pos.ZERO is compatible with Point
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