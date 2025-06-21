package nub.wi1helm.inventory;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.Material;
import nub.wi1helm.inventory.item.RaceMenuItem;
import nub.wi1helm.server.ServerPlayer;
import nub.wi1helm.template.inventory.TemplateInventory;

public class DefaultInventory extends TemplateInventory {
    public DefaultInventory() {
        super(Component.text("Player Inventory"), InventoryType.CHEST_1_ROW);
    }

    @Override
    protected void initialize() {
    }

    @Override
    protected void personalize(Player player) {
        final ServerPlayer p = (ServerPlayer) player;

        switch (p.getServerTeam()) {
            case AQUA_CREEPERS -> {setItem(8,new RaceMenuItem(Material.CREEPER_SPAWN_EGG));}
            case PURPLE_AXOLOTLS -> {setItem(8,new RaceMenuItem(Material.AXOLOTL_SPAWN_EGG));}
            default -> {setItem(8,new RaceMenuItem(Material.NETHER_STAR));}
        }

    }
}
