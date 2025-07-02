package nub.wi1helm.listener;

import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class InstanceHandler {

    EventNode<InstanceEvent> node = EventNode.type("instance", EventFilter.INSTANCE);

    static InstanceHandler instance;

    private InstanceHandler(EventNode<Event> global) {
        global.addChild(this.node);
    }

    @NotNull
    public static InstanceHandler getInstance(EventNode<Event> global) {
        if (instance == null) return new InstanceHandler(global);
        return instance;
    }

    @Nullable
    public static InstanceHandler getInstance() {
        return instance;
    }
}
