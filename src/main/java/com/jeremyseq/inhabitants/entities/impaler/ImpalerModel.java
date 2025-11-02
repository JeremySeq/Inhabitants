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
    @Override
    public ResourceLocation getModelResource(ImpalerEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/impaler.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ImpalerEntity animatable) {
        if (animatable.getTextureType() == 0) {
            return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/impaler.png");
        } else {
            return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/impaler_dripstone.png");
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
