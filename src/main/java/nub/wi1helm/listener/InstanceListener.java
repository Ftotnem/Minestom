package nub.wi1helm.listener;

import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class InstanceListener {

    EventNode<InstanceEvent> node = EventNode.type("instance", EventFilter.INSTANCE);

    static InstanceListener instance;

    private InstanceListener(EventNode<Event> global) {
        global.addChild(this.node);
    }

    @NotNull
    public static InstanceListener getInstance(EventNode<Event> global) {
        if (instance == null) return new InstanceListener(global);
        return instance;
    }

    @Nullable
    public static InstanceListener getInstance() {
        return instance;
    }
}
