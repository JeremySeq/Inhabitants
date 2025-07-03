package com.jeremyseq.inhabitants.entities.impaler.spike;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ImpalerSpikeModel extends GeoModel<ImpalerSpikeProjectile> {
    @Override
    public ResourceLocation getModelResource(ImpalerSpikeProjectile impalerSpikeProjectile) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/impaler_spike.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ImpalerSpikeProjectile impalerSpikeProjectile) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/impaler_spike.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ImpalerSpikeProjectile impalerSpikeProjectile) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/warped_clam.animation.json");
    }
}
