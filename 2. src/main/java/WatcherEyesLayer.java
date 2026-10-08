package com.watcher.thewatcher;

import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Makes the face (eyes + grin) glow in the dark. */
public class WatcherEyesLayer extends EyesLayer<WatcherEntity, EndermanModel<WatcherEntity>> {
    private static final RenderType GLOW = RenderType.eyes(
            new ResourceLocation(TheWatcher.MODID, "textures/entity/watcher_eyes.png"));

    public WatcherEyesLayer(RenderLayerParent<WatcherEntity, EndermanModel<WatcherEntity>> parent) {
        super(parent);
    }

    @Override
    public RenderType renderType() {
        return GLOW;
    }
}
