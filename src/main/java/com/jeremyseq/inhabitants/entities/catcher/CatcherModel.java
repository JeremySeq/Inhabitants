package com.jeremyseq.inhabitants.entities.catcher;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CatcherModel extends GeoModel<CatcherEntity> {
    @Override
    public ResourceLocation getModelResource(CatcherEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/catcher.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CatcherEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/catcher.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CatcherEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/catcher.animation.json");
    }
}
