package com.jeremyseq.inhabitants.blocks.impaler_head;

import com.jeremyseq.inhabitants.Inhabitants;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

public class ImpalerHeadModel extends GeoModel<ImpalerHeadBlockEntity> {
    @Override
    public ResourceLocation getModelResource(ImpalerHeadBlockEntity animatable) {
        if (animatable.getBlockState().hasProperty(ImpalerWallHeadBlock.FACING)) {
            return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID,
                "geo/impaler_head_wall.geo.json");
        }

        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID,
            "geo/impaler_head.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ImpalerHeadBlockEntity animatable) {
        return switch (animatable.getVariant()) {
            case DEFAULT -> ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/impaler/impaler.png");
            case DRIPSTONE -> ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/impaler/impaler_dripstone.png");
            case ALBINO -> ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/impaler/impaler_albino.png");
            case FORLORN_HOLLOWS -> ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/impaler/impaler_forlorn_hollows.png");
        };
    }

    @Override
    public ResourceLocation getAnimationResource(ImpalerHeadBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID,
            "animations/impaler_head.animation.json");
    }
}
