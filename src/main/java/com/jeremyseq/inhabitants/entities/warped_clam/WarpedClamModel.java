package com.jeremyseq.inhabitants.entities.warped_clam;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class WarpedClamModel extends GeoModel<WarpedClamEntity> {
    @Override
    public ResourceLocation getModelResource(WarpedClamEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/warped_clam.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WarpedClamEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/warped_clam.png");
    }

    @Override
    public ResourceLocation getAnimationResource(WarpedClamEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/warped_clam.animation.json");
    }

    @Override
    public void setCustomAnimations(WarpedClamEntity animatable, long instanceId, AnimationState<WarpedClamEntity> animationState) {
        getAnimationProcessor().getRegisteredBones().forEach(bone -> {
            if (bone.getName().equals("pearl")) {
                bone.setHidden(!animatable.hasPearl());
            }
        });
    }
}
