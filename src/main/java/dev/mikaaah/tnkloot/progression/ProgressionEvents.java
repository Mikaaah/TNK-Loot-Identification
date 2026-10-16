package dev.mikaaah.tnkloot.progression;

import dev.mikaaah.tnkloot.config.TnkConfig;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class ProgressionEvents {
    private ProgressionEvents() {}

    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (TnkConfig.get().mode() != TnkConfig.Mode.ADVANCEMENTS) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var id = event.getAdvancement().id();
        for (var tier : TnkConfig.get().tiers()) {
            if (tier.advancements().contains(id)) ProgressionService.unlock(player, tier.rank());
        }
    }

    /** Handles players who completed vanilla advancements before installing this mod. */
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (TnkConfig.get().mode() != TnkConfig.Mode.ADVANCEMENTS) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        reconcile(player);
    }

    public static void reconcile(ServerPlayer player) {
        if (TnkConfig.get().mode() != TnkConfig.Mode.ADVANCEMENTS) return;
        int highest = ProgressionService.getTier(player);
        for (var tier : TnkConfig.get().tiers()) {
            for (var advancementId : tier.advancements()) {
                AdvancementHolder adv = player.getServer().getAdvancements().get(advancementId);
                if (adv != null && player.getAdvancements().getOrStartProgress(adv).isDone()) {
                    highest = Math.max(highest, tier.rank());
                }
            }
        }
        if (highest > ProgressionService.getTier(player)) ProgressionService.unlock(player, highest);
    }
}