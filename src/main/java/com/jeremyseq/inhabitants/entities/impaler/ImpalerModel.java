package com.jeremyseq.inhabitants.entities.impaler;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ImpalerModel extends GeoModel<ImpalerEntity> {
    @Override
    public ResourceLocation getModelResource(ImpalerEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/impaler.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ImpalerEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/impaler.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ImpalerEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/impaler.animation.json");
    }
}
