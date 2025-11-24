package com.jeremyseq.inhabitants.entities.dryfang.throwing_knife;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ThrowingKnifeModel extends GeoModel<ThrowingKnifeProjectile> {
    @Override
    public ResourceLocation getModelResource(ThrowingKnifeProjectile throwingKnifeProjectile) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/throwing_knife.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ThrowingKnifeProjectile throwingKnifeProjectile) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/throwing_knife.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ThrowingKnifeProjectile throwingKnifeProjectile) {
        return null;
    }
}
