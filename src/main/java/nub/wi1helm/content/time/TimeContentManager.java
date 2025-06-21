// src/main/java/nub/wi1helm/content/time/TimeContentManager.java
package nub.wi1helm.content.time;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType; // Not directly used here, but for content creation
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.SendablePacket; // Not used here
import nub.wi1helm.Main;
import nub.wi1helm.content.npc.TutorialNPC;
import nub.wi1helm.server.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function; // Could also use Supplier<TimeContent>

public class TimeContentManager {
    // This list now holds factories (blueprints), not actual TimeContent instances
    private final List<TimeContentFactory> contentFactories = new ArrayList<>();

    // Private constructor: register your content factories here
    private TimeContentManager() {
        // Register factories that know how to create your content
        register(player -> new TutorialNPC(Main.instance, new Pos(-4.5, 0, 3.5), 0)); // Example: Tutorial NPC Factory
        register(player -> new TimeBlock(Block.EMERALD_BLOCK, new BlockVec(-3, 0, 4), 50.0)); // Example: Time Block Factory

        // If your content creation needs the instance, define the factory like this:
        // register(instance -> new TutorialNPC(instance, new Pos(-4.5, 0, 3.5), 0));
        // You would then modify create() in TimeContentFactory to take an instance.
    }

    // Holder idiom for lazy-loaded thread-safe singleton
    private static class Holder {
        private static final TimeContentManager INSTANCE = new TimeContentManager();
    }

    public static TimeContentManager getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Registers a factory that can create instances of TimeContent.
     *
     * @param factory The factory responsible for creating TimeContent.
     */
    public void register(TimeContentFactory factory) {
        contentFactories.add(factory);
    }

    /**
     * Retrieves all registered TimeContent factories.
     * @return A list of TimeContent factories.
     */
    public List<TimeContentFactory> getContentFactories() {
        return contentFactories;
    }

    // getSpawnPackets and getDespawnPackets are no longer here.
    // They will move to the ServerPlayer or a per-player content manager.
}