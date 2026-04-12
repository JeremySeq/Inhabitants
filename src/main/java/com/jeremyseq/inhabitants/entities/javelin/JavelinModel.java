package com.jeremyseq.inhabitants.entities.javelin;

import com.jeremyseq.inhabitants.Inhabitants;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

public class JavelinModel extends GeoModel<JavelinEntity> {
    @Override
    public ResourceLocation getModelResource(JavelinEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/javelin.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(JavelinEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/javelin_texture.png");
    }

    @Override
    public ResourceLocation getAnimationResource(JavelinEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/javelin.animation.json");
    }
}
