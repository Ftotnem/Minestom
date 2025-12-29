package nub.wi1helm.inventory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.Material;
import nub.wi1helm.inventory.item.TimeSplashPotionItem;
import nub.wi1helm.server.ServerPlayer; // Assuming ServerPlayer is your custom player class
import nub.wi1helm.template.inventory.TemplateInventory;
import nub.wi1helm.template.inventory.TemplateInventoryEvent;
import nub.wi1helm.template.inventory.TemplateItem;
import nub.wi1helm.template.inventory.items.BackgroundItem;
import nub.wi1helm.template.inventory.items.CloseButton;

import java.util.List;

public class TimeExchangeBankMenu extends TemplateInventory {

    public TimeExchangeBankMenu() {
        super(MiniMessage.miniMessage().deserialize("<dark_purple>Time Exchange Bank</dark_purple>"), InventoryType.CHEST_4_ROW);
    }

    @Override
    protected void initialize() {
        // Close button in the bottom center
        setItem(31, new CloseButton());
        // Fill empty slots with a background item
        fillInventory(new BackgroundItem());
    }

    @Override
    protected void personalize(Player player) {
        // Ensure the player is your custom ServerPlayer
        if (!(player instanceof ServerPlayer p)) {
            player.sendMessage(Component.text("Error: Not a ServerPlayer instance.", NamedTextColor.RED));
            return;
        }

        // Display current time and time/second
        setItem(4, new TemplateItem(Material.CLOCK) {
            @Override
            protected void initialize() {
                setName(MiniMessage.miniMessage().deserialize("<gold>Your Time Balance</gold>"));
            }

            @Override
            protected void personalize(Player player) {
                final ServerPlayer p1 = (ServerPlayer) player;
                // Assuming ServerPlayer has methods to get playtime and delta time
                // You'll need to replace these with your actual methods
                double currentPlaytime = p1.getCurrentPlaytime() / 20; // Placeholder method
                double currentDeltaTime = p1.getDeltaPlaytime(); // Placeholder method

                setLore(List.of(
                        MiniMessage.miniMessage().deserialize("<gray>Current Total Playtime:</gray> <yellow>" + String.format("%.2f", currentPlaytime) + "s</yellow>"),
                        MiniMessage.miniMessage().deserialize("<gray>Current Time/Second (s/s):</gray> <green>" + String.format("%.2f", currentDeltaTime) + " s/s</green>"),
                        Component.empty(),
                        MiniMessage.miniMessage().deserialize("<gray>Use your time to purchase boosts!</gray>")
                ));
            }

            @Override
            public void onUse(TemplateInventoryEvent event) {
                // No action on clicking the info item
            }

            @Override
            public void onDrop(TemplateInventoryEvent event) {
                // No action on dropping
            }
        });


        setItem(10, new TimeSplashPotionItem(10));
        setItem(12, new TimeSplashPotionItem(100));
        setItem(14, new TimeSplashPotionItem(1000));
        setItem(16, new TimeSplashPotionItem(10000));
    }
}