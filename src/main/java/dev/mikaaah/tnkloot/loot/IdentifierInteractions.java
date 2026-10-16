package dev.mikaaah.tnkloot.loot;

import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.menu.TnkMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class IdentifierInteractions {
    private static final String PREFIX = "tnkloot.identifier.";
    private IdentifierInteractions() {}
    public static boolean isIdentifier(Entity entity) {
        return entity instanceof Villager && entity.getTags().stream().anyMatch(t -> t.startsWith(PREFIX));
    }
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!isIdentifier(event.getTarget())) return;
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player && TnkConfig.get().stationIdentification()) {
            BlockPos anchor = anchor(event.getTarget());
            if (anchor != null && player.level().getBlockState(anchor).getBlock() instanceof IdentifierBlock)
                TnkMenuProvider.openIdentifier(player, anchor);
        }
    }
    private static BlockPos anchor(Entity entity) {
        String tag = entity.getTags().stream().filter(t -> t.startsWith(PREFIX)).findFirst().orElse("");
        try {
            String[] coordinates = tag.substring(PREFIX.length()).split("\\.");
            return new BlockPos(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]), Integer.parseInt(coordinates[2]));
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) { return null; }
    }
    public static void onAttack(AttackEntityEvent event) {
        if (isIdentifier(event.getTarget())) event.setCanceled(true);
    }
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (isIdentifier(event.getEntity())) event.setCanceled(true);
    }
    public static void onDeath(LivingDeathEvent event) {
        if (isIdentifier(event.getEntity())) { event.setCanceled(true); event.getEntity().setHealth(event.getEntity().getMaxHealth()); }
    }
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Villager v) || !(v.level() instanceof ServerLevel level) || !isIdentifier(v)) return;
        BlockPos pos = anchor(v);
        if (pos == null || !level.hasChunkAt(pos)) return;
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof IdentifierBlock)) {
            IdentifierBlock.particles(level, v, false); v.discard(); return;
        }
        IdentifierBlock.position(v, pos, state);
    }
}
