package dev.mikaaah.tnkloot.progression;

import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.config.TnkConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.Optional;

/** Reflection keeps FTB Teams genuinely optional; failure falls back to player-only progress. */
public final class TeamResolver {
    private static boolean warned;
    private TeamResolver() {}

    public static Optional<String> teamKey(ServerPlayer player) {
        if (!TnkConfig.get().shareFtbTeams() || !ModList.get().isLoaded("ftbteams")) return Optional.empty();
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
            Object api = apiClass.getMethod("api").invoke(null);
            Class<?> apiInterface = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API");
            if (!(boolean) apiInterface.getMethod("isManagerLoaded").invoke(api)) return Optional.empty();
            Object manager = apiInterface.getMethod("getManager").invoke(api);
            Method lookup = Class.forName("dev.ftb.mods.ftbteams.api.TeamManager").getMethod("getTeamForPlayerID", java.util.UUID.class);
            Object result = lookup.invoke(manager, player.getUUID());
            if (!(result instanceof Optional<?> optional) || optional.isEmpty()) return Optional.empty();
            Object team = optional.get();
            Object id = Class.forName("dev.ftb.mods.ftbteams.api.Team").getMethod("getId").invoke(team);
            return Optional.of("team:" + id);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            if (!warned) {
                warned = true;
                TnkLoot.LOG.warn("Optional FTB Teams API lookup unavailable; using personal tiers", ex);
            }
            return Optional.empty();
        }
    }
}