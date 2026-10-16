package dev.mikaaah.tnkloot.client;

import dev.mikaaah.tnkloot.TnkLoot;
import dev.mikaaah.tnkloot.menu.TnkMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = TnkLoot.MOD_ID, value = Dist.CLIENT)
public final class TnkClientMenus {
    private TnkClientMenus() {}
    @net.neoforged.bus.api.SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        event.register(TnkMenus.IDENTIFY.get(), TnkMenuScreen::new);
        event.register(TnkMenus.ADMIN.get(), TnkMenuScreen::new);
    }
}
