package dev.mikaaah.tnkloot;

import dev.mikaaah.tnkloot.config.TnkConfig.Category;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

/** All player-facing blocks and items, available together in Creative inventory. */
public final class TnkCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TnkLoot.MOD_ID);
    public static final Supplier<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.tnkloot"))
        .icon(() -> new ItemStack(TnkLoot.UNIDENTIFIED_WEAPON.get()))
        .displayItems((parameters, output) -> {
            output.accept(TnkLoot.IDENTIFIER_ITEM.get());
            for (Category category : Category.values())
                output.accept(UnidentifiedWeaponItem.create(category, 0));
        })
        .build());

    private TnkCreativeTabs() {}
}
