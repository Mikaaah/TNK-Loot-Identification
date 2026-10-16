package dev.mikaaah.tnkloot.menu;

import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.IContainerFactory;

/** Button driven menu shared by the identification counter and operator tier editor. */
public final class TnkMenu extends AbstractContainerMenu {
    public static final IContainerFactory<TnkMenu> IDENTIFY_FACTORY = (id, inv, buf) -> new TnkMenu(TnkMenus.IDENTIFY.get(), id, inv, false, TnkConfig.parseTierSnapshot(buf.readUtf(32767)));
    public static final IContainerFactory<TnkMenu> ADMIN_FACTORY = (id, inv, buf) -> new TnkMenu(TnkMenus.ADMIN.get(), id, inv, true, TnkConfig.parseTierSnapshot(buf.readUtf(32767)));
    private final boolean admin;
    private final net.minecraft.world.inventory.ContainerData data = new net.minecraft.world.inventory.SimpleContainerData(1);
    private final java.util.List<TnkConfig.Tier> displayTiers;

    public TnkMenu(MenuType<?> type, int id, Inventory inventory, boolean admin) {
        this(type, id, inventory, admin, TnkConfig.get().tiers());
    }
    public TnkMenu(MenuType<?> type, int id, Inventory inventory, boolean admin, java.util.List<TnkConfig.Tier> displayTiers) {
        super(type, id); this.admin = admin; this.displayTiers = displayTiers;
        addDataSlots(data);
    }
    @Override public boolean stillValid(Player player) { return true; }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    public boolean admin() { return admin; }
    public int selectedIndex() { return data.get(0); }
    public java.util.List<TnkConfig.Tier> displayTiers() { return displayTiers; }
    public TnkConfig.Tier selectedTier() {
        var tiers = TnkConfig.get().tiers();
        return tiers.get(Math.floorMod(selectedIndex(), tiers.size()));
    }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer server)) return true;
        if (!admin) {
            if (id == 0) identify(server);
            return true;
        }
        if (!server.hasPermissions(2)) { server.sendSystemMessage(Component.literal("Operator permission required.")); return true; }
        switch (id) {
            case 0 -> identify(server);
            case 1 -> { data.set(0, Math.floorMod(selectedIndex() - 1, TnkConfig.get().tiers().size())); report(server); }
            case 2 -> { data.set(0, (selectedIndex() + 1) % TnkConfig.get().tiers().size()); report(server); }
            case 3 -> cycleRarity(server);
            case 4 -> addHeld(server);
            case 5 -> removeHeld(server);
            case 6 -> addTier(server);
        }
        return true;
    }
    private void identify(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack token = player.getInventory().getItem(i);
            if (token.getItem() instanceof UnidentifiedWeaponItem) {
                var result = UnidentifiedWeaponItem.identify(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND, token);
                if (result.getResult().consumesAction()) player.getInventory().setItem(i, result.getObject());
                return;
            }
        }
        player.sendSystemMessage(Component.translatable("message.tnkloot.station_instruction"));
    }
    private void cycleRarity(ServerPlayer player) {
        var t = selectedTier();
        var all = TnkConfig.get().tiers().stream().map(TnkConfig.Tier::apotheosisRarity).distinct().toList();
        int next = (all.indexOf(t.apotheosisRarity()) + 1) % all.size();
        TnkConfig.editTier(t.rank(), null, all.get(next)); report(player);
    }
    private void addHeld(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) { player.sendSystemMessage(Component.literal("Hold an item to add it to this tier.")); return; }
        var category = TnkConfig.get().weapons().stream().filter(w -> w.id().equals(BuiltInRegistries.ITEM.getKey(held.getItem()).toString()))
            .map(TnkConfig.Weapon::category).findFirst().orElseGet(() -> inferCategory(held));
        boolean ok = TnkConfig.addWeaponToTier(BuiltInRegistries.ITEM.getKey(held.getItem()).toString(), category, selectedTier().rank());
        player.sendSystemMessage(Component.literal(ok ? "Added item to tier " + selectedTier().id() : "Could not save the item pool."));
    }
    private static TnkConfig.Category inferCategory(ItemStack stack) {
        var item = stack.getItem();
        if (item instanceof net.minecraft.world.item.BowItem || item instanceof net.minecraft.world.item.CrossbowItem) return TnkConfig.Category.BOW;
        if (item instanceof net.minecraft.world.item.ArmorItem armor) return switch (armor.getEquipmentSlot()) {
            case HEAD -> TnkConfig.Category.HELMET; case CHEST -> TnkConfig.Category.CHESTPLATE;
            case LEGS -> TnkConfig.Category.LEGGINGS; case FEET -> TnkConfig.Category.BOOTS; default -> TnkConfig.Category.ACCESSORY;
        };
        if (item instanceof net.minecraft.world.item.DiggerItem) return TnkConfig.Category.TOOL;
        return TnkConfig.Category.WEAPON;
    }
    private void removeHeld(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) { player.sendSystemMessage(Component.literal("Hold an item to remove it from this tier.")); return; }
        String id = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        boolean removed = false;
        for (var category : TnkConfig.Category.values()) if (TnkConfig.removeWeaponFromTier(id, category, selectedTier().rank())) removed = true;
        player.sendSystemMessage(Component.literal(removed ? "Removed item from this tier." : "That item has no tier-specific entry to remove."));
    }
    private void addTier(ServerPlayer player) {
        int rank = TnkConfig.get().highestTier() + 1;
        var rarity = selectedTier().apotheosisRarity();
        boolean ok = TnkConfig.addTier("tier_" + rank, "Tier " + rank, rarity);
        if (ok) data.set(0, TnkConfig.get().tiers().size() - 1);
        player.sendSystemMessage(Component.literal(ok ? "Created Tier " + rank + ". Rename and tune advancements in tiers.json." : "Could not save tier."));
    }
    private void report(ServerPlayer player) {
        var t = selectedTier(); player.sendSystemMessage(Component.literal("Editing " + t.name() + " (rank " + t.rank() + "), Apotheosis group " + t.apotheosisRarity()));
    }
}
