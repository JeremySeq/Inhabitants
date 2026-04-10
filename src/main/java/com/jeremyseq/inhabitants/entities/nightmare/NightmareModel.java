package com.jeremyseq.inhabitants.entities.nightmare;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class NightmareModel extends GeoModel<NightmareEntity> {
    @Override
    public ResourceLocation getModelResource(NightmareEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/nightmare.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(NightmareEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/nightmare/nightmare_0.png");
    }

    @Override
    public ResourceLocation getAnimationResource(NightmareEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/nightmare.animation.json");
    }
}
