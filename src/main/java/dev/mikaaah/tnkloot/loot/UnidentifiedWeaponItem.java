package dev.mikaaah.tnkloot.loot;

import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

public final class UnidentifiedWeaponItem extends Item {
    public UnidentifiedWeaponItem(Item.Properties properties) { super(properties); }

    /** The tier is SNAPSHOTTED at creation, not recalculated when redeemed. */
    public static ItemStack create(int rank) {
        ItemStack stack = new ItemStack(TnkLoot.UNIDENTIFIED_WEAPON.get());
        CompoundTag tag = new CompoundTag();
        int boundedTier = TnkConfig.get().tierForRank(rank).rank();
        tag.putInt("tier", boundedTier);
        tag.putString("tier_name", TnkConfig.get().tierForRank(boundedTier).name());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static int tier(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : Math.max(0, data.copyTag().getInt("tier"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.pass(stack);
        if (!TnkConfig.get().enableIdentification()) {
            serverPlayer.sendSystemMessage(Component.translatable("message.tnkloot.disabled"));
            return InteractionResultHolder.fail(stack);
        }
        int rank = tier(stack);
        ItemStack result = WeaponSelector.roll(level, rank);
        if (result.isEmpty()) {
            serverPlayer.sendSystemMessage(Component.translatable("message.tnkloot.empty_pool", rank));
            return InteractionResultHolder.fail(stack);
        }
        serverPlayer.sendSystemMessage(Component.translatable("message.tnkloot.identified", result.getHoverName()).withStyle(ChatFormatting.GREEN));
        if (stack.getCount() == 1) return InteractionResultHolder.success(result);
        // Also preserve extra tokens if a command overrides the normal stack size.
        stack.shrink(1);
        if (!serverPlayer.getInventory().add(result)) serverPlayer.drop(result, false);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int rank = tier(stack);
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag payload = data == null ? new CompoundTag() : data.copyTag();
        String name = payload.contains("tier_name") ? payload.getString("tier_name") : TnkConfig.get().tierForRank(rank).name();
        tooltip.add(Component.translatable("tooltip.tnkloot.tier", rank, name).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.tnkloot.identify").withStyle(ChatFormatting.GRAY));
    }
}