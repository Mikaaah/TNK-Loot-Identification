package dev.mikaaah.tnkloot.progression;

import dev.mikaaah.tnkloot.config.TnkConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ProgressionService {
    private ProgressionService() {}

    public static String playerKey(ServerPlayer player) { return "player:" + player.getUUID(); }

    public static int getTier(ServerPlayer player) {
        var cfg = TnkConfig.get();
        if (cfg.mode() == TnkConfig.Mode.OFF) return cfg.highestTier();
        var data = ProgressionData.of(player.getServer());
        int personal = data.get(playerKey(player));
        int effective = TeamResolver.teamKey(player).map(k -> Math.max(personal, data.get(k))).orElse(personal);
        return cfg.tierForRank(effective).rank();
    }

    /** Monotonic unlock; individual and effective party are both preserved. */
    public static boolean unlock(ServerPlayer player, int rank) {
        int boundedRank = TnkConfig.get().tierForRank(rank).rank();
        var data = ProgressionData.of(player.getServer());
        boolean advanced = data.unlock(playerKey(player), boundedRank);
        var team = TeamResolver.teamKey(player);
        if (team.isPresent()) advanced |= data.unlock(team.get(), boundedRank);
        if (advanced) {
            String name = TnkConfig.get().tierForRank(boundedRank).name();
            player.sendSystemMessage(Component.translatable("message.tnkloot.unlocked", boundedRank, name));
        }
        return advanced;
    }

    /** Explicit admin rollback: team and player, so a team rank cannot immediately restore an old value. */
    public static void reset(ServerPlayer player) {
        var data = ProgressionData.of(player.getServer());
        data.set(playerKey(player), 0);
        TeamResolver.teamKey(player).ifPresent(k -> data.set(k, 0));
    }
}