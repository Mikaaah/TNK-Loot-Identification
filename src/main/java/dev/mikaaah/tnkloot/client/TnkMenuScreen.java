package dev.mikaaah.tnkloot.client;

import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.menu.TnkMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class TnkMenuScreen extends AbstractContainerScreen<TnkMenu> {
    public TnkMenuScreen(TnkMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 320; imageHeight = 190;
        inventoryLabelY = 10000;
    }
    @Override protected void init() {
        super.init();
        int x = leftPos, y = topPos;
        if (menu.admin()) {
            addButton("◀ Tier", x + 12, y + 52, 64, 20, 1);
            addButton("Tier ▶", x + 82, y + 52, 64, 20, 2);
            addButton("Cycle group", x + 152, y + 52, 90, 20, 3);
            addButton("Add held item", x + 12, y + 82, 100, 20, 4);
            addButton("Remove held", x + 118, y + 82, 94, 20, 5);
            addButton("+ New tier", x + 218, y + 82, 90, 20, 6);
            addButton("Identify token", x + 12, y + 132, 110, 20, 0);
        } else {
            addButton("Identify held / inventory token", x + 74, y + 105, 172, 24, 0);
        }
    }
    private void addButton(String label, int x, int y, int width, int height, int action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
        }).bounds(x, y, width, height).build());
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF171C27);
        graphics.fill(leftPos + 5, topPos + 5, leftPos + imageWidth - 5, topPos + imageHeight - 5, 0xFF252D3B);
        graphics.fill(leftPos + 8, topPos + 8, leftPos + imageWidth - 8, topPos + 28, 0xFF3B3020);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 14, 14, 0xFFF3D28A, false);
        if (!menu.admin()) {
            graphics.drawString(font, Component.literal("Place an unidentified item in your inventory."), 20, 47, 0xFFE0E5EF, false);
            var costs = TnkConfig.get().costs();
            graphics.drawString(font, Component.literal(costs.enabled() ? "Cost: " + costs.defaultAmount() + " " + costs.item() : "Identification is free"), 20, 68, 0xFFC0C8D8, false);
            graphics.drawString(font, Component.literal("A failed attempt keeps your unidentified item."), 20, 84, 0xFF9AA5B5, false);
        } else {
            var tiers = menu.displayTiers();
            var tier = tiers.get(Math.min(Math.max(0, menu.selectedIndex()), tiers.size() - 1));
            graphics.drawString(font, Component.literal("Tier " + tier.rank() + " · " + tier.id() + " · " + tier.name()), 14, 36, 0xFFE0E5EF, false);
            graphics.drawString(font, Component.literal("Apotheosis group: " + tier.apotheosisRarity()), 14, 112, 0xFFC0C8D8, false);
            graphics.drawString(font, Component.literal("Hold equipment, then add/remove it from this tier."), 14, 124, 0xFF9AA5B5, false);
            graphics.drawString(font, Component.literal("Names, IDs and advancement unlocks remain editable in tiers.json."), 14, 154, 0xFF9AA5B5, false);
        }
    }
}
