package com.jeremyseq.inhabitants.entities.impaler.arrow;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ConcussionArrowModel extends GeoModel<ConcussionArrowProjectile> {
    @Override
    public ResourceLocation getModelResource(ConcussionArrowProjectile animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/concussion_arrow.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ConcussionArrowProjectile animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/concussion_arrow.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ConcussionArrowProjectile animatable) {
        return null;
    }
}
