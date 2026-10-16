package dev.mikaaah.tnkloot.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.chat.Component;

/** Identification counter with a persistent, immobile villager attendant. */
public final class IdentifierBlock extends Block {
    private static String npcTag(BlockPos pos) { return "tnkloot.identifier." + pos.getX() + "." + pos.getY() + "." + pos.getZ(); }
    public IdentifierBlock(Properties properties) { super(properties); }

    @Override public ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!dev.mikaaah.tnkloot.config.TnkConfig.get().stationIdentification()) {
            serverPlayer.sendSystemMessage(Component.translatable("message.tnkloot.station_disabled"));
            return ItemInteractionResult.FAIL;
        }
        dev.mikaaah.tnkloot.menu.TnkMenuProvider.openIdentifier(serverPlayer);
        return ItemInteractionResult.SUCCESS;
    }

    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && dev.mikaaah.tnkloot.config.TnkConfig.get().stationIdentification())
            dev.mikaaah.tnkloot.menu.TnkMenuProvider.openIdentifier(serverPlayer);
        return InteractionResult.SUCCESS;
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;
        AABB area = new AABB(pos).inflate(1.8);
        String tag = npcTag(pos);
        boolean exists = !serverLevel.getEntitiesOfClass(Villager.class, area, v -> v.getTags().contains(tag)).isEmpty();
        if (exists) return;
        Villager villager = EntityType.VILLAGER.create(serverLevel);
        if (villager == null) return;
        villager.moveTo(pos.getX() + 1.4, pos.getY(), pos.getZ() + 0.5, 180, 0);
        villager.setNoAi(true);
        villager.setInvulnerable(true);
        villager.setPersistenceRequired();
        villager.setCustomName(Component.translatable("entity.tnkloot.identifier"));
        villager.setCustomNameVisible(true);
        villager.addTag(tag);
        serverLevel.addFreshEntity(villager);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            String tag = npcTag(pos);
            serverLevel.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(1.8), v -> v.getTags().contains(tag))
                .forEach(Villager::discard);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
