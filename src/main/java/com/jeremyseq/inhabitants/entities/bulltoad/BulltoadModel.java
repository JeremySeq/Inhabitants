package com.jeremyseq.inhabitants.entities.bulltoad;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.HashMap;
import java.util.Map;

public class BulltoadModel extends GeoModel<BulltoadEntity> {
    private final Map<Integer, Float> tongueScales = new HashMap<>();
    private final Map<Integer, Float> hornScales = new HashMap<>();

    @Override
    public ResourceLocation getModelResource(BulltoadEntity animatable) {
        if (animatable.isBaby()) {
            return ResourceLocation.fromNamespaceAndPath("inhabitants", "geo/baby_bulltoad.geo.json");
        }
        return ResourceLocation.fromNamespaceAndPath("inhabitants", "geo/bulltoad.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BulltoadEntity animatable) {
        if (animatable.isBaby()) {
            return ResourceLocation.fromNamespaceAndPath("inhabitants", "textures/entity/bulltoad/baby_bulltoad.png");
        } else if (animatable.getBreedStage() == 1) {
            return ResourceLocation.fromNamespaceAndPath("inhabitants", "textures/entity/bulltoad/withtoads_0.png");
        } else if (animatable.getBreedStage() == 2) {
            return ResourceLocation.fromNamespaceAndPath("inhabitants", "textures/entity/bulltoad/withtoads_1.png");
        }
        return ResourceLocation.fromNamespaceAndPath("inhabitants", "textures/entity/bulltoad/bulltoad.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BulltoadEntity animatable) {
        if (animatable.isBaby()) {
            return ResourceLocation.fromNamespaceAndPath("inhabitants", "animations/baby_bulltoad.animation.json");
        }
        return ResourceLocation.fromNamespaceAndPath("inhabitants", "animations/bulltoad.animation.json");
    }

    @Override
    public void setCustomAnimations(BulltoadEntity animatable, long instanceId, AnimationState<BulltoadEntity> animationState) {
        CoreGeoBone head = getAnimationProcessor().getBone("Bulltoad");
        if (head != null) {
            if (animatable.shouldSnapYaw()) {
                EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
                head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
                head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
            }
        }

        CoreGeoBone tongue = getAnimationProcessor().getBone("tongue");
        if (tongue != null) {
            tongue.setScaleZ(1f);
            tongue.setRotX(0f);

            if (animatable.isTongueOut()) {
                Vec3 targetVec = animatable.getTongueTarget();
                Vec3 eyePos = animatable.getEyePosition();

                Vec3 diff = targetVec.subtract(eyePos);
                float targetDistance = (float) diff.length();

                float current = tongueScales.getOrDefault(animatable.getId(), 0f);
                float speed = targetDistance > current ? 0.4f : 0.2f;
                current += (targetDistance - current) * speed;
                tongueScales.put(animatable.getId(), current);

                float pitch = (float) Math.toDegrees(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)));
                tongue.setRotX((float) Math.toRadians(pitch));
                tongue.setScaleZ(current);
            } else {
                tongueScales.put(animatable.getId(), 0f);
            }
        }


        // show or hide horns
        // lerp scale when growing horns
        float targetHornScale = animatable.hasHorns() ? 1f : 0f;
        float currentHornScale = hornScales.getOrDefault(animatable.getId(), 0f);
        float speed = targetHornScale > currentHornScale ? 0.02f : 1f; // if growing horns, do it gradually, if losing horns, do it immediately
        currentHornScale += (targetHornScale - currentHornScale) * speed;
        hornScales.put(animatable.getId(), currentHornScale);

        float finalCurrentHornScale = currentHornScale;
        getAnimationProcessor().getRegisteredBones().forEach(bone -> {
            if (bone.getName().contains("horns")) {
                bone.setHidden(finalCurrentHornScale <= 0.01f);
                bone.setScaleX(finalCurrentHornScale);
                bone.setScaleY(finalCurrentHornScale);
                bone.setScaleZ(finalCurrentHornScale);
            }
        });
    }
}