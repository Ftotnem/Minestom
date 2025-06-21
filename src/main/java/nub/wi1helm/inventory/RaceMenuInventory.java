package nub.wi1helm.inventory;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import nub.wi1helm.template.inventory.TemplateInventory;
import nub.wi1helm.template.inventory.items.BackgroundItem;

public class RaceMenuInventory extends TemplateInventory {
    public RaceMenuInventory() {
        super(Component.text("Race Menu [RM]"), InventoryType.CHEST_5_ROW);
    }

    @Override
    protected void initialize() {
        fillInventory(new BackgroundItem());
    }

    @Override
    protected void personalize(Player player) {

    }
}
