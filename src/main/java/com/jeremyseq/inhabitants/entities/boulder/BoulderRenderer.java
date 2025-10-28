package com.jeremyseq.inhabitants.entities.boulder;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BoulderRenderer extends GeoEntityRenderer<BoulderEntity> {
    public BoulderRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BoulderModel());
        this.shadowRadius = .5f;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull BoulderEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/boulder.png");
    }
}
