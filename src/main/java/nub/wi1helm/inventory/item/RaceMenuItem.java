package nub.wi1helm.inventory.item;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import nub.wi1helm.inventory.RaceMenuInventory;
import nub.wi1helm.template.inventory.TemplateInventoryEvent;
import nub.wi1helm.template.inventory.TemplateItem;

public class RaceMenuItem extends TemplateItem {
    public RaceMenuItem(Material material) {
        super(material);
    }

    @Override
    protected void initialize() {
        setName(Component.text("Race Menu [RM]"));
    }

    @Override
    protected void personalize(Player player) {

    }

    @Override
    public void onUse(TemplateInventoryEvent event) {
        event.getPlayer().openInventory(new RaceMenuInventory().constructInventory(event.getPlayer()));
    }

    @Override
    public void onDrop(TemplateInventoryEvent event) {

    }
}
