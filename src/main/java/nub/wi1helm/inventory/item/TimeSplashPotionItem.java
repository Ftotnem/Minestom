package nub.wi1helm.inventory.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.item.Material;
import nub.wi1helm.template.inventory.TemplateInventoryEvent;
import nub.wi1helm.template.inventory.TemplateItem;

public class TimeSplashPotionItem extends TemplateItem {

    private final int time;
    public TimeSplashPotionItem(int time) {
        super(Material.PLAYER_HEAD);
        this.time = time;
    }

    @Override
    protected void initialize() {

    }

    @Override
    protected void personalize(Player player) {
        setName(MiniMessage.miniMessage().deserialize("<aqua>Splash Potion Of " + time + "s</aqua>"));
        setSkin(new PlayerSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWFmODE1MWRlYjM5NTUzMjkyYjlhNjk2NzQ4MzJkZGE4MDAxNzgwY2E5ODE4MGEwNmVhZjliMzY4OTMwYjViYiJ9fX0=",""));
    }

    @Override
    public void onUse(TemplateInventoryEvent event) {

    }

    @Override
    public void onDrop(TemplateInventoryEvent event) {

    }
}
