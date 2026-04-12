package com.jeremyseq.inhabitants.items.javelin;

import com.jeremyseq.inhabitants.Inhabitants;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

public class JavelinItemModel extends GeoModel<JavelinItem> {
    @Override
    public ResourceLocation getModelResource(JavelinItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/javelin_hand.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(JavelinItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/javelin_texture.png");
    }

    @Override
    public ResourceLocation getAnimationResource(JavelinItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/javelin_hand.animation.json");
    }
}
