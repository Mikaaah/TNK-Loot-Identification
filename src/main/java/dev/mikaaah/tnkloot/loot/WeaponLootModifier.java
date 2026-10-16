package dev.mikaaah.tnkloot.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.progression.ProgressionService;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** Replaces configured weapons in entity, chest and other generated loot with tier-stamped tokens. */
public final class WeaponLootModifier extends LootModifier {
    public static final MapCodec<WeaponLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
        LootModifier.codecStart(inst).apply(inst, WeaponLootModifier::new));

    public WeaponLootModifier(LootItemCondition[] conditions) { super(conditions); }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!TnkConfig.get().replaceWeaponLoot() || !TnkConfig.get().enableIdentification()) return generatedLoot;
        ServerPlayer player = playerFrom(context);
        int rank = player == null ? 0 : ProgressionService.getTier(player);
        for (int i = 0; i < generatedLoot.size(); i++) {
            ItemStack stack = generatedLoot.get(i);
            boolean configured = TnkConfig.get().weapons().stream().anyMatch(w ->
                BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(net.minecraft.resources.ResourceLocation.parse(w.id()))
                    && WeaponSelector.available(w));
            if (!configured) continue;
            generatedLoot.remove(i--);
            for (int n = 0; n < stack.getCount(); n++) generatedLoot.add(UnidentifiedWeaponItem.create(rank));
        }
        return generatedLoot;
    }

    private static ServerPlayer playerFrom(LootContext context) {
        Entity entity = context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER);
        if (entity instanceof ServerPlayer player) return player;
        entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (entity instanceof ServerPlayer player) return player;
        entity = context.getParamOrNull(LootContextParams.ATTACKING_ENTITY);
        return entity instanceof ServerPlayer player ? player : null;
    }

    @Override public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
