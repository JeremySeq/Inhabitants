package com.jeremyseq.inhabitants.entities.warped_clam;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WarpedClamModel extends GeoModel<WarpedClamEntity> {
    @Override
    public ResourceLocation getModelResource(WarpedClamEntity bogreEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/warped_clam.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WarpedClamEntity bogreEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/warped_clam.png");
    }

    @Override
    public ResourceLocation getAnimationResource(WarpedClamEntity bogreEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/warped_clam.animation.json");
    }
}
