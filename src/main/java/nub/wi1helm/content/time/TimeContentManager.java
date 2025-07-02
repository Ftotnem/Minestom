package nub.wi1helm.content.time;// nub.wi1helm.content.time.TimeContentManager

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import nub.wi1helm.Main;
import nub.wi1helm.content.entity.SignTimeEntity;
import nub.wi1helm.content.npc.DailyEventNPC;
import nub.wi1helm.content.npc.TimeExchangeNPC;
import nub.wi1helm.content.npc.TutorialNPC;

import java.util.ArrayList;
import java.util.List;

public class TimeContentManager {
    private final List<ContentFactoryEntry> contentFactories = new ArrayList<>();

    private TimeContentManager() {
        // Register factories with a unique string identifier for each registration
        // These identifiers will be used in the map key later
        register("tutorial_npc", player -> new TutorialNPC(Main.instance, new Pos(-4.5, 0, -3.5), 0));
        register("emerald_block_tutorial", player -> new TimeBlock(Block.EMERALD_BLOCK, new BlockVec(-4, 0, -5), 50.0));
        register("daily_event_npc", player -> new DailyEventNPC(Main.instance, new Pos(0,0,22), 20));
        register("daily_challenge_sign_1", player -> new SignTimeEntity(Main.instance, new Pos(0,8,21), 0, MiniMessage.miniMessage().deserialize("<yellow><bold>DAILY CHALLENGE!</bold></yellow>"), new Vec(4,4,4)));
        register("time_exchange_sign_1", player -> new SignTimeEntity(Main.instance, new Pos(2,6,-18), 0, MiniMessage.miniMessage().deserialize("<gold><bold>TIME EXCHANGE!</bold></gold>"), new Vec(4,4,4)));
        register("time_exchange_npc", player -> new TimeExchangeNPC(Main.instance, new Pos(2.5,0,-19.5), 0));
        // You could even register another SignTimeEntity if needed, with a different unique identifier:
        // register("another_sign_2", player -> new SignTimeEntity(Main.instance, new Pos(10,10,10), 0, Component.text("Another Sign"), new Vec(2,2,2)));
    }

    // Holder idiom remains the same
    private static class Holder {
        private static final TimeContentManager INSTANCE = new TimeContentManager();
    }

    public static TimeContentManager getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Registers a factory with a unique identifier.
     * The identifier should be unique for each *registration* of a content factory.
     */
    public void register(String identifier, TimeContentFactory factory) {
        contentFactories.add(new ContentFactoryEntry(identifier, factory));
    }

    /**
     * Retrieves all registered TimeContent factories with their identifiers.
     * @return A list of ContentFactoryEntry.
     */
    public List<ContentFactoryEntry> getContentFactories() {
        return contentFactories;
    }
}