package com.jeremyseq.inhabitants.entities.bulltoad;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BulltoadModel extends GeoModel<BulltoadEntity> {
    @Override
    public ResourceLocation getModelResource(BulltoadEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("inhabitants", "geo/bulltoad.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BulltoadEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("inhabitants", "textures/entity/bulltoad/bulltoad.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BulltoadEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("inhabitants", "animations/bulltoad.animation.json");
    }
}
