package com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BogreCauldronRenderer extends GeoEntityRenderer<BogreCauldronEntity> {
    public BogreCauldronRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BogreCauldronModel());
    }

    @Override
    public ResourceLocation getTextureLocation(BogreCauldronEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/bogre_cauldron.png");
    }
}
