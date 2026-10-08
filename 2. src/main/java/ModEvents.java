package com.watcher.thewatcher;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ModEvents {

    @Mod.EventBusSubscriber(modid = TheWatcher.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Common {
        @SubscribeEvent
        public static void attributes(EntityAttributeCreationEvent event) {
            event.put(ModEntities.WATCHER.get(), WatcherEntity.createAttributes().build());
        }
    }

    @Mod.EventBusSubscriber(modid = TheWatcher.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Client {
        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.WATCHER.get(), WatcherRenderer::new);
        }
    }
}
