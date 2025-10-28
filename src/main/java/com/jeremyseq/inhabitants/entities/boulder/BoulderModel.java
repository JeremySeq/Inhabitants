package com.jeremyseq.inhabitants.entities.boulder;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BoulderModel extends GeoModel<BoulderEntity> {
    @Override
    public ResourceLocation getModelResource(BoulderEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/boulder.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BoulderEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/boulder.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BoulderEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/boulder.animation.json");
    }
}
