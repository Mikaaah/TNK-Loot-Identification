package dev.mikaaah.tnkloot.client;

import dev.mikaaah.tnkloot.menu.TnkMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Compact vanilla-style counter: seven inputs, a currency icon and confirmation. */
public final class TnkMenuScreen extends AbstractContainerScreen<TnkMenu> {
    private Button confirm;
    public TnkMenuScreen(TnkMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = menu.admin() ? 320 : 176;
        imageHeight = menu.admin() ? 190 : 156;
        inventoryLabelY = menu.admin() ? 10000 : 62;
        titleLabelX = 8; titleLabelY = 6;
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
        } else confirm = addButton("✓", x + 150, y + 29, 19, 19, 0);
    }
    private Button addButton(String label, int x, int y, int width, int height, int action) {
        return addRenderableWidget(Button.builder(Component.literal(label), b -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
        }).bounds(x, y, width, height).build());
    }
    @Override protected void containerTick() {
        super.containerTick();
        if (confirm != null) {
            confirm.active = !menu.busy() && menu.affordable() && menu.inputCount() > 0;
            confirm.setTooltip(Tooltip.create(Component.translatable("menu.tnkloot.confirm",
                menu.inputCount(), menu.quotedCost(), Component.translatable(menu.currency().getDescriptionId()))));
        }
    }
    private void slotBackground(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        g.fill(x, y, x + 17, y + 17, 0xFFFFFFFF);
        g.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF373737);
        g.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFFFFFFF);
        g.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + imageHeight - 3, 0xFFC6C6C6);
        if (!menu.admin()) {
            for (var slot : menu.slots) slotBackground(g, leftPos + slot.x, topPos + slot.y);
            g.renderItem(new ItemStack(menu.currency()), leftPos + 134, topPos + 30);
            g.fill(leftPos + 7, topPos + 59, leftPos + 169, topPos + 60, 0xFFAAAAAA);
        }
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        if (!menu.admin()) {
            g.drawString(font, Component.translatable(menu.busy() ? "menu.tnkloot.rolling" : "menu.tnkloot.insert"), 8, 19, 0x404040, false);
            if (menu.inputCount() > 0 || menu.busy())
                g.drawString(font, Component.translatable("menu.tnkloot.total", menu.quotedCost()), 8, 50,
                    menu.busy() || menu.affordable() ? 0x285A25 : 0xAA3333, false);
        } else {
            var tiers = menu.displayTiers();
            var tier = tiers.get(Math.floorMod(menu.selectedIndex(), tiers.size()));
            g.drawString(font, Component.literal("Tier " + tier.rank() + " · " + tier.id() + " · " + tier.name()), 14, 36, 0x404040, false);
            g.drawString(font, Component.literal("Apotheosis group: " + tier.apotheosisRarity()), 14, 112, 0x404040, false);
            g.drawString(font, Component.literal("Hold equipment to add/remove it from this tier."), 14, 132, 0x404040, false);
            g.drawString(font, Component.literal("Edit names and unlocks in tiers.json."), 14, 154, 0x404040, false);
        }
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (!menu.busy()) renderTooltip(g, mouseX, mouseY);
    }
}
