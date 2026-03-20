package com.jeremyseq.inhabitants.entities.concher;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ConcherModel extends GeoModel<ConcherEntity> {
    @Override
    public ResourceLocation getModelResource(ConcherEntity concherEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/concher_3.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ConcherEntity concherEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/concher_3.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ConcherEntity concherEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/concher_3.animation.json");
    }
}
