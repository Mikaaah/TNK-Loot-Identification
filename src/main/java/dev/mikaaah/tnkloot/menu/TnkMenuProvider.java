package dev.mikaaah.tnkloot.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public final class TnkMenuProvider {
    private TnkMenuProvider() {}
    public static void openIdentifier(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new TnkMenu(TnkMenus.IDENTIFY.get(), id, inv, false),
            Component.translatable("menu.tnkloot.identifier")), buf -> buf.writeUtf(dev.mikaaah.tnkloot.config.TnkConfig.tierSnapshot()));
    }
    public static void openEditor(ServerPlayer player) {
        if (!player.hasPermissions(2)) { player.sendSystemMessage(Component.literal("Operator permission required.")); return; }
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new TnkMenu(TnkMenus.ADMIN.get(), id, inv, true),
            Component.translatable("menu.tnkloot.editor")), buf -> buf.writeUtf(dev.mikaaah.tnkloot.config.TnkConfig.tierSnapshot()));
    }
}
