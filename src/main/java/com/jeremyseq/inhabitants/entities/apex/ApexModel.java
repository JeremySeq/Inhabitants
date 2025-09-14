package com.jeremyseq.inhabitants.entities.apex;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ApexModel extends GeoModel<ApexEntity> {
    @Override
    public ResourceLocation getModelResource(ApexEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/apex.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ApexEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/apex.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ApexEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/apex.animation.json");
    }
}
