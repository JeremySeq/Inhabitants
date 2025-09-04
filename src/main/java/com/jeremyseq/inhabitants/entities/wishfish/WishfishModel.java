package com.jeremyseq.inhabitants.entities.wishfish;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class WishfishModel extends GeoModel<WishfishEntity> {
    @Override
    public ResourceLocation getModelResource(WishfishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/wishfish.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WishfishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/wishfish.png");
    }

    @Override
    public ResourceLocation getAnimationResource(WishfishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/wishfish.animation.json");
    }

    @Override
    public void setCustomAnimations(WishfishEntity animatable, long instanceId, AnimationState<WishfishEntity> animationState) {
        CoreGeoBone rootBone = getAnimationProcessor().getBone("wishfish");
        if (rootBone != null && !animatable.isInWater()) {
            rootBone.setRotZ((float) Math.toRadians(90));
        } else if (rootBone != null) {
            rootBone.setRotZ(0);
        }
    }
}
