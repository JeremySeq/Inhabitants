package com.jeremyseq.inhabitants.entities.bogre;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BogreModel extends GeoModel<BogreEntity> {
    @Override
    public ResourceLocation getModelResource(BogreEntity bogreEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/bogre.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BogreEntity bogreEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/bogre.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BogreEntity bogreEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/bogre.animation.json");
    }
}
