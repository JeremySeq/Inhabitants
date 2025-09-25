package com.jeremyseq.inhabitants.blocks.entity.client;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.blocks.entity.GazerPodBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class GazerPodBlockModel extends GeoModel<GazerPodBlockEntity> {

    @Override
    public ResourceLocation getModelResource(GazerPodBlockEntity gazerPodBlockEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/gazer_pod_block.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GazerPodBlockEntity gazerPodBlockEntity) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/block/gazer_pod_block.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GazerPodBlockEntity gazerPodBlockEntity) {
        return null;
    }

    @Override
    public void setCustomAnimations(GazerPodBlockEntity animatable, long instanceId, AnimationState<GazerPodBlockEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        CoreGeoBone gazerBone = this.getAnimationProcessor().getBone("gazer");
        if (gazerBone != null) {
            gazerBone.setHidden(!animatable.hasGazer());
        }
    }
}
