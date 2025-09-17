package com.jeremyseq.inhabitants.entities.gazer_pod;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GazerPodModel extends GeoModel<GazerPodEntity> {

    @Override
    public ResourceLocation getModelResource(GazerPodEntity gazerPodEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/gazer_pod.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GazerPodEntity gazerPodEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/gazer_pod.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GazerPodEntity gazerPodEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/gazer_pod.animation.json");
    }
}
