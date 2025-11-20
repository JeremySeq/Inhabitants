package com.jeremyseq.inhabitants.blocks.entity.client;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.blocks.ScrollBlock;
import com.jeremyseq.inhabitants.blocks.entity.ScrollBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ScrollBlockModel extends GeoModel<ScrollBlockEntity> {

    @Override
    public ResourceLocation getModelResource(ScrollBlockEntity scrollBlockEntity) {
        return switch (scrollBlockEntity.getBlockState().getValue(ScrollBlock.SIZE)) {
            case 0 -> ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/scroll_small.geo.json");
            case 2 -> ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/scroll_large.geo.json");
            default -> ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/scroll_medium.geo.json");
        };
    }

    @Override
    public ResourceLocation getTextureResource(ScrollBlockEntity scrollBlockEntity) {
        return switch (scrollBlockEntity.getBlockState().getValue(ScrollBlock.SIZE)) {
            case 0 -> ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/block/scroll_small.png");
            case 2 -> ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/block/scroll_large.png");
            default -> ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/block/scroll_medium.png");
        };
    }

    @Override
    public ResourceLocation getAnimationResource(ScrollBlockEntity scrollBlockEntity) {
        return switch (scrollBlockEntity.getBlockState().getValue(ScrollBlock.SIZE)) {
            case 0 -> ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/scroll_small.animation.json");
            case 2 -> ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/scroll_large.animation.json");
            default -> ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/scroll_medium.animation.json");
        };
    }
}
