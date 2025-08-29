package com.jeremyseq.inhabitants.entities.gazer;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GazerModel extends GeoModel<GazerEntity> {
    @Override
    public ResourceLocation getModelResource(GazerEntity gazerEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/gazer.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GazerEntity gazerEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/gazer.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GazerEntity gazerEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/gazer.animation.json");
    }
}
