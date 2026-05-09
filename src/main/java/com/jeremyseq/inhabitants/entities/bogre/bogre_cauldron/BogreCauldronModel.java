package com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron;

import com.jeremyseq.inhabitants.Inhabitants;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

public class BogreCauldronModel extends GeoModel<BogreCauldronEntity> {
    private static final int FRAMES = 5; // number of frames in animation
    private static final int FRAME_TIME = 4; // ticks per frame

    @Override
    public ResourceLocation getModelResource(BogreCauldronEntity object) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/bogre_cauldron.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BogreCauldronEntity object) {
        int animationIndex = (object.tickCount / FRAME_TIME) % FRAMES;

        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, String.format("textures/entity/bogre_cauldron/bogre_cauldron_%d.png", animationIndex));
    }

    @Override
    public ResourceLocation getAnimationResource(BogreCauldronEntity bogreEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/bogre_cauldron.animation.json");
    }
}
