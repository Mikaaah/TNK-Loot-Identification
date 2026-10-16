package dev.mikaaah.tnkloot.menu;

import dev.mikaaah.tnkloot.TnkLoot;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class TnkMenus {
    private TnkMenus() {}
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, TnkLoot.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<TnkMenu>> IDENTIFY = MENUS.register("identify", () -> IMenuTypeExtension.create(TnkMenu.IDENTIFY_FACTORY));
    public static final DeferredHolder<MenuType<?>, MenuType<TnkMenu>> ADMIN = MENUS.register("tier_editor", () -> IMenuTypeExtension.create(TnkMenu.ADMIN_FACTORY));
}
