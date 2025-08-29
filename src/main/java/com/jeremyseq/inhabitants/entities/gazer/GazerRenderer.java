package com.jeremyseq.inhabitants.entities.gazer;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GazerRenderer extends GeoEntityRenderer<GazerEntity> {
    public GazerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GazerModel());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull GazerEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/gazer.png");
    }
}
