package com.jeremyseq.inhabitants.entities.bogre;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BogreRenderer extends GeoEntityRenderer<BogreEntity> {
    public BogreRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BogreModel());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(BogreEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/bogre.png");
    }
}
