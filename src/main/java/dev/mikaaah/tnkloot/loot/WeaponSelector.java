package dev.mikaaah.tnkloot.loot;

import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.config.TnkConfig.Category;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/** Missing optional mod items never resolve to air or consume an unidentified item. */
public final class WeaponSelector {
    private WeaponSelector() {}

    public static boolean available(TnkConfig.Weapon weapon) {
        ResourceLocation id = ResourceLocation.parse(weapon.id());
        return BuiltInRegistries.ITEM.containsKey(id) && BuiltInRegistries.ITEM.get(id) != Items.AIR;
    }
    public static WeightedPool<Item> pool(int tier) {
        return pool(tier, Category.WEAPON);
    }
    public static WeightedPool<Item> pool(int tier, Category category) {
        List<WeightedPool.Entry<Item>> entries = new ArrayList<>();
        for (var weapon : TnkConfig.get().weapons()) {
            if (weapon.category() == category && tier >= weapon.minTier() && tier <= weapon.maxTier() && available(weapon))
                entries.add(new WeightedPool.Entry<>(BuiltInRegistries.ITEM.get(ResourceLocation.parse(weapon.id())), weapon.weight()));
        }
        return new WeightedPool<>(entries);
    }
    public static ItemStack roll(Level level, int tier) {
        return roll(level, tier, Category.WEAPON);
    }
    public static ItemStack roll(Level level, int tier, Category category) {
        return roll(level, tier, category, level.getRandom());
    }
    public static ItemStack roll(Level level, int tier, Category category, RandomSource random) {
        return pool(tier, category).roll(random::nextLong).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }
}
