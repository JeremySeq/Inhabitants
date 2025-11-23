package com.jeremyseq.inhabitants.entities.dryfang;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DryfangModel extends GeoModel<DryfangEntity> {
    @Override
    public ResourceLocation getModelResource(DryfangEntity dryfangEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/dryfang.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DryfangEntity dryfangEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/dryfang.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DryfangEntity dryfangEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/dryfang.animation.json");
    }
}
