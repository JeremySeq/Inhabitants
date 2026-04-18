package com.jeremyseq.inhabitants.entities.bulltoad;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BulltoadRenderer extends GeoEntityRenderer<BulltoadEntity> {
    public BulltoadRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BulltoadModel());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull BulltoadEntity animatable) {
        if (animatable.getBreedStage() == 1) {
            return ResourceLocation.fromNamespaceAndPath("inhabitants", "textures/entity/bulltoad/withtoads_0.png");
        } else if (animatable.getBreedStage() == 2) {
            return ResourceLocation.fromNamespaceAndPath("inhabitants", "textures/entity/bulltoad/withtoads_1.png");
        }
        return ResourceLocation.fromNamespaceAndPath("inhabitants", "textures/entity/bulltoad/bulltoad.png");
    }
}
