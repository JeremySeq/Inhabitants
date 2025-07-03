package com.jeremyseq.inhabitants.entities.impaler;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class ImpalerRenderer extends GeoEntityRenderer<ImpalerEntity> {
    public ImpalerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ImpalerModel());
        this.shadowRadius = 0.5f;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ImpalerEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/impaler.png");
    }
}
