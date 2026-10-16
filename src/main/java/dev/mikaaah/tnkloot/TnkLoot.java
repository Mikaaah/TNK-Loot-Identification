package dev.mikaaah.tnkloot;

import dev.mikaaah.tnkloot.command.TnkCommands;
import dev.mikaaah.tnkloot.config.TnkConfig;
import dev.mikaaah.tnkloot.loot.UnidentifiedWeaponItem;
import dev.mikaaah.tnkloot.progression.ProgressionEvents;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(TnkLoot.MOD_ID)
public final class TnkLoot {
    public static final String MOD_ID = "tnkloot";
    public static final Logger LOG = LogUtils.getLogger();
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredItem<UnidentifiedWeaponItem> UNIDENTIFIED_WEAPON =
        ITEMS.registerItem("unidentified_weapon", UnidentifiedWeaponItem::new, new Item.Properties().stacksTo(1));

    public TnkLoot(IEventBus modBus) {
        ITEMS.register(modBus);
        TnkConfig.reload();
        NeoForge.EVENT_BUS.addListener(ProgressionEvents::onAdvancement);
        NeoForge.EVENT_BUS.addListener(ProgressionEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(TnkCommands::register);
    }
}