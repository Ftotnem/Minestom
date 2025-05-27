package nub.wi1helm.server;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Sidebar;
import net.minestom.server.timer.TaskSchedule;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerSidebar {
    private static final Map<UUID, Sidebar> sidebarCache = new HashMap<>();

    public static void create() {
        MinecraftServer.getSchedulerManager().submitTask(() -> {
            for (Player p : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                if (!(p instanceof ServerPlayer player)) continue;

                // Get or create the sidebar for the player
                Sidebar sidebar = sidebarCache.get(player.getUuid());
                if (sidebar == null) {
                    // Create new sidebar if it doesn't exist
                    sidebar = new Sidebar(MiniMessage.miniMessage().deserialize("<gradient:#00d2ff:#3a47d5><bold>Race To 10 Million</bold></gradient>"));

                    // Attach the sidebar to the player
                    sidebar.addViewer(player);

                    // Cache the sidebar for future updates#ff75aa
                    sidebarCache.put(player.getUuid(), sidebar);
                } else {
                    //hello
                }
            }
            return TaskSchedule.tick(1); // Run every tick for smoother animation
        });
    }
    public static Map<UUID, Sidebar> getSidebarCache() {
        return sidebarCache;
    }
}