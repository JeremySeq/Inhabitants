package com.jeremyseq.inhabitants.entities.impaler;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class ImpalerModel extends GeoModel<ImpalerEntity> {
    private static final int FRAMES = 5; // number of frames in animation
    private static final int FRAME_TIME = 3; // ticks per frame

    @Override
    public ResourceLocation getModelResource(ImpalerEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/impaler.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ImpalerEntity animatable) {
        int animationIndex = (animatable.tickCount / FRAME_TIME) % FRAMES;

        if (animatable.getTextureType() == 0) {
            return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, String.format("textures/entity/impaler/impaler_%d.png", animationIndex));
        } else {
            return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, String.format("textures/entity/impaler/impaler_dripstone_%d.png", animationIndex));
        }
    }

    @Override
    public ResourceLocation getAnimationResource(ImpalerEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/impaler.animation.json");
    }

    @Override
    public void setCustomAnimations(ImpalerEntity animatable, long instanceId, AnimationState<ImpalerEntity> animationState) {
        CoreGeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
        }


        boolean hideSpikes = !animatable.isSpiked();
        getAnimationProcessor().getRegisteredBones().forEach(bone -> {
            if (bone.getName().contains("spikes")) {
                bone.setHidden(hideSpikes);
            }
        });
    }
}
