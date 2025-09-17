package com.jeremyseq.inhabitants.entities.gazer_pod;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GazerPodRenderer extends GeoEntityRenderer<GazerPodEntity> {
    public GazerPodRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GazerPodModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public ResourceLocation getTextureLocation(GazerPodEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/gazer_pod.png");
    }
}
