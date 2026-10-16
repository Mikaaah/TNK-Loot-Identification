package dev.mikaaah.tnkloot.loot;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class IdentifierInteractions {
    private IdentifierInteractions() {}
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager) || !villager.getTags().stream().anyMatch(t -> t.startsWith("tnkloot.identifier."))) return;
        if (event.getEntity() instanceof ServerPlayer player && dev.mikaaah.tnkloot.config.TnkConfig.get().stationIdentification()) {
            dev.mikaaah.tnkloot.menu.TnkMenuProvider.openIdentifier(player);
            event.setCanceled(true);
        }
    }
}
