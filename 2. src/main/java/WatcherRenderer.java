package com.watcher.thewatcher;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class WatcherRenderer extends MobRenderer<WatcherEntity, EndermanModel<WatcherEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(TheWatcher.MODID, "textures/entity/watcher.png");

    public WatcherRenderer(EntityRendererProvider.Context context) {
        super(context, new EndermanModel<>(context.bakeLayer(ModelLayers.ENDERMAN)), 1.5F);
        this.addLayer(new WatcherEyesLayer(this));
    }

    @Override
    protected void scale(WatcherEntity entity, PoseStack poseStack, float partialTick) {
        // Vanilla enderman is ~2.9 blocks tall; 2.76x makes it 8 blocks.
        poseStack.scale(2.76F, 2.76F, 2.76F);
    }

    @Override
    public ResourceLocation getTextureLocation(WatcherEntity entity) {
        return TEXTURE;
    }
}
