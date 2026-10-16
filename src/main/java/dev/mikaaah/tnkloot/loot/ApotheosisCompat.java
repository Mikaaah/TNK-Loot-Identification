package dev.mikaaah.tnkloot.loot;

import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Optional bridge to Apotheosis' public loot-generation API; no Apotheosis classes are linked at startup. */
public final class ApotheosisCompat {
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private ApotheosisCompat() {}

    public static ItemStack apply(ItemStack stack, int rank, Player player) {
        if (!ModList.get().isLoaded("apotheosis")) return stack;
        try {
            ClassLoader loader = ApotheosisCompat.class.getClassLoader();
            Class<?> rarityRegistry = Class.forName("dev.shadowsoffire.apotheosis.loot.RarityRegistry", true, loader);
            Object registry = rarityRegistry.getField("INSTANCE").get(null);
            @SuppressWarnings("unchecked")
            List<Object> rarities = (List<Object>) rarityRegistry.getMethod("getSortedRarities").invoke(null);
            ResourceLocation wanted = TnkConfig.get().tierForRank(rank).apotheosisRarity();
            Object rarity = null;
            for (Object candidate : rarities) {
                for (Method method : registry.getClass().getMethods()) {
                    if (method.getName().equals("getKey") && method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isAssignableFrom(candidate.getClass())) {
                        Object key = method.invoke(registry, candidate);
                        if (wanted.equals(key)) rarity = candidate;
                        break;
                    }
                }
                if (rarity != null) break;
            }
            if (rarity == null) {
                warn("Apotheosis rarity " + wanted + " is not registered; returning an unmodified weapon", null);
                return stack;
            }
            Class<?> genContext = Class.forName("dev.shadowsoffire.apotheosis.tiers.GenContext", true, loader);
            Object context = genContext.getMethod("forPlayer", Player.class).invoke(null, player);
            Class<?> lootController = Class.forName("dev.shadowsoffire.apotheosis.loot.LootController", true, loader);
            return (ItemStack) lootController.getMethod("createLootItem", ItemStack.class, rarity.getClass(), genContext)
                .invoke(null, stack, rarity, context);
        } catch (ReflectiveOperationException | LinkageError ex) {
            warn("Apotheosis integration failed; returning an unmodified weapon", ex);
            return stack;
        }
    }

    private static void warn(String message, Throwable error) {
        if (WARNED.compareAndSet(false, true)) {
            if (error == null) TnkLoot.LOG.warn(message);
            else TnkLoot.LOG.warn(message, error);
        }
    }
}
