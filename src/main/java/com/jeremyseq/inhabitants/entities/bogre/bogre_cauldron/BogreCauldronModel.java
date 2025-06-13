package com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BogreCauldronModel extends GeoModel<BogreCauldronEntity> {
    @Override
    public ResourceLocation getModelResource(BogreCauldronEntity object) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/bogre_cauldron.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BogreCauldronEntity object) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/bogre_cauldron.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BogreCauldronEntity bogreEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/bogre_cauldron.animation.json");
    }
}
