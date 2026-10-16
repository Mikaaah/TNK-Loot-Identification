package dev.mikaaah.tnkloot.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.config.TnkConfig.Category;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.mikaaah.tnkloot.loot.WeaponSelector;
import dev.mikaaah.tnkloot.progression.ProgressionEvents;
import dev.mikaaah.tnkloot.progression.ProgressionService;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

public final class TnkCommands {
    private TnkCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tnkloot")
            .then(Commands.literal("tier")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    int rank = ProgressionService.getTier(p);
                    ctx.getSource().sendSuccess(() -> Component.literal("Guild tier: " + rank + " (" +
                        TnkConfig.get().tierForRank(rank).name() + "); progression mode " + TnkConfig.get().mode()), false);
                    return rank + 1;
                })
                .then(Commands.literal("list").executes(ctx -> {
                    for (var tier : TnkConfig.get().tiers()) {
                        ctx.getSource().sendSuccess(() -> Component.literal(tier.rank() + " / " + tier.id() + " — " + tier.name()), false);
                    }
                    return TnkConfig.get().tiers().size();
                }))
                .then(Commands.literal("unlock").requires(source -> source.hasPermission(2))
                    .then(Commands.argument("players", EntityArgument.players())
                        .then(Commands.argument("tier", StringArgumentType.word())
                            .executes(ctx -> change(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"),
                                StringArgumentType.getString(ctx, "tier"))))))
                .then(Commands.literal("reset").requires(source -> source.hasPermission(2))
                    .then(Commands.argument("players", EntityArgument.players())
                        .executes(ctx -> {
                            Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
                            players.forEach(ProgressionService::reset);
                            ctx.getSource().sendSuccess(() -> Component.literal("Reset Guild tier for " + players.size() + " target(s)"), true);
                            return players.size();
                        }))))
            .then(Commands.literal("reload").requires(source -> source.hasPermission(2)).executes(ctx -> {
                boolean ok = TnkConfig.reload();
                if (ok) ctx.getSource().getServer().getPlayerList().getPlayers().forEach(ProgressionEvents::reconcile);
                ctx.getSource().sendSuccess(() -> Component.literal(ok ? "T&K loot config reloaded" : "Invalid config; old settings preserved"), true);
                return ok ? 1 : 0;
            }))
            .then(Commands.literal("validate").requires(source -> source.hasPermission(2)).executes(ctx -> {
                int problems = 0;
                for (var weapon : TnkConfig.get().weapons()) if (!WeaponSelector.available(weapon)) {
                    ctx.getSource().sendFailure(Component.literal("Unavailable item: " + weapon.id()));
                    problems++;
                }
                for (var tier : TnkConfig.get().tiers()) if (WeaponSelector.pool(tier.rank()).isEmpty()) {
                    ctx.getSource().sendFailure(Component.literal("Empty live weapon pool: " + tier.id()));
                    problems++;
                }
                for (Category category : TnkConfig.get().weapons().stream().map(TnkConfig.Weapon::category).distinct().toList())
                    for (var tier : TnkConfig.get().tiers()) if (WeaponSelector.pool(tier.rank(), category).isEmpty()) {
                        ctx.getSource().sendFailure(Component.literal("Empty " + category.name().toLowerCase() + " pool: " + tier.id()));
                        problems++;
                    }
                int count = problems;
                ctx.getSource().sendSuccess(() -> Component.literal("Validation: " + count + " issue(s)"), false);
                return problems == 0 ? 1 : 0;
            }))
            .then(Commands.literal("unidentified").requires(source -> source.hasPermission(2))
                .then(Commands.argument("players", EntityArgument.players())
                    .executes(ctx -> give(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"), null, Category.WEAPON))
                    .then(Commands.argument("tier", StringArgumentType.word())
                        .executes(ctx -> give(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"),
                            StringArgumentType.getString(ctx, "tier"), Category.WEAPON))
                        .then(Commands.argument("category", StringArgumentType.word())
                            .executes(ctx -> give(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"),
                                StringArgumentType.getString(ctx, "tier"), parseCategory(StringArgumentType.getString(ctx, "category"))))))
                    .then(Commands.argument("category", StringArgumentType.word())
                        .executes(ctx -> give(ctx.getSource(), EntityArgument.getPlayers(ctx, "players"), null,
                            parseCategory(StringArgumentType.getString(ctx, "category"))))))));
    }

    private static int parseTier(String token) {
        try {
            int rank = Integer.parseInt(token);
            if (TnkConfig.get().tiers().stream().anyMatch(t -> t.rank() == rank)) return rank;
        } catch (NumberFormatException ignored) { }
        return TnkConfig.get().tierForName(token).map(TnkConfig.Tier::rank).orElse(-1);
    }

    private static int change(net.minecraft.commands.CommandSourceStack source,
                              Collection<ServerPlayer> players, String tierToken) {
        int rank = parseTier(tierToken);
        if (rank < 0) {
            source.sendFailure(Component.literal("Unknown tier '" + tierToken + "'. Use /tnkloot tier list"));
            return 0;
        }
        int count = 0;
        for (var player : players) if (ProgressionService.unlock(player, rank)) count++;
        int changes = count;
        source.sendSuccess(() -> Component.literal("Unlocked tier " + rank + " for " + changes + " target(s)"), true);
        return count;
    }

    private static int give(net.minecraft.commands.CommandSourceStack source,
                            Collection<ServerPlayer> players, String tierToken, Category category) {
        if (category == null) {
            source.sendFailure(Component.literal("Unknown category; use weapon, bow, helmet, chestplate, leggings, boots, tool or accessory"));
            return 0;
        }
        int rank = tierToken == null ? -1 : parseTier(tierToken);
        if (tierToken != null && rank < 0) {
            source.sendFailure(Component.literal("Unknown tier '" + tierToken + "'"));
            return 0;
        }
        for (var player : players) {
            int effective = tierToken == null ? ProgressionService.getTier(player) : rank;
            var stack = UnidentifiedWeaponItem.create(category, effective);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
        source.sendSuccess(() -> Component.literal("Given unidentified " + category.name().toLowerCase() + " tokens to " + players.size() + " target(s)"), true);
        return players.size();
    }

    private static Category parseCategory(String raw) {
        try { return Category.parse(raw); }
        catch (IllegalArgumentException ex) { return null; }
    }
}
