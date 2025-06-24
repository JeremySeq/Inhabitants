package com.jeremyseq.inhabitants.entities.abyssfish;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AbyssfishModel extends GeoModel<AbyssfishEntity> {
    @Override
    public ResourceLocation getModelResource(AbyssfishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/abyssfish.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AbyssfishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/abyssfish.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AbyssfishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/abyssfish.animation.json");
    }
}
