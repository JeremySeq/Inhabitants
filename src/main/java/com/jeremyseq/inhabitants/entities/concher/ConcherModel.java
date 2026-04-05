package com.jeremyseq.inhabitants.entities.concher;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ConcherModel extends GeoModel<ConcherEntity> {
    @Override
    public ResourceLocation getModelResource(ConcherEntity concherEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, String.format("geo/concher_%d.geo.json", concherEntity.getStage()));
    }

    @Override
    public ResourceLocation getTextureResource(ConcherEntity concherEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, String.format("textures/entity/concher_%d.png", concherEntity.getStage()));
    }

    @Override
    public ResourceLocation getAnimationResource(ConcherEntity concherEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, String.format("animations/concher_%d.animation.json", concherEntity.getStage()));
    }
}
