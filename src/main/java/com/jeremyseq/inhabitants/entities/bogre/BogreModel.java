package com.jeremyseq.inhabitants.entities.bogre;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class BogreModel extends GeoModel<BogreEntity> {
    @Override
    public ResourceLocation getModelResource(BogreEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/bogre.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BogreEntity animatable) {
        if (animatable.getTextureType() == 0) {
            return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/bogre.png");
        } else {
            return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/bogre2.png");
        }
    }

    @Override
    public ResourceLocation getAnimationResource(BogreEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "animations/bogre.animation.json");
    }

    @Override
    public void setCustomAnimations(BogreEntity animatable, long instanceId, AnimationState<BogreEntity> animationState) {
        CoreGeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
        }
    }
}
