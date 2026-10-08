package com.watcher.thewatcher;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TheWatcher.MODID)
public class TheWatcher {
    public static final String MODID = "thewatcher";

    public TheWatcher() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITIES.register(bus);
    }
}
