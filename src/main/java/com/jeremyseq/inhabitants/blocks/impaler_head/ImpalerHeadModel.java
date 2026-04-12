package com.jeremyseq.inhabitants.blocks.impaler_head;

import com.jeremyseq.inhabitants.Inhabitants;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

public class ImpalerHeadModel extends GeoModel<ImpalerHeadBlockEntity> {
    @Override
    public ResourceLocation getModelResource(ImpalerHeadBlockEntity animatable) {
        if (animatable.getBlockState().hasProperty(ImpalerWallHeadBlock.FACING)) {
            return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID,
                "geo/impaler_wall_head.geo.json");
        }

        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID,
            "geo/impaler_head.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ImpalerHeadBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID,
            "textures/entity/impaler_head/impaler_head_texture.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ImpalerHeadBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID,
            "animations/impaler_head.animation.json");
    }
}
