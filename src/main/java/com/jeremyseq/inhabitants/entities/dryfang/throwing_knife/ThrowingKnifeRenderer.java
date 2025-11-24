package com.jeremyseq.inhabitants.entities.dryfang.throwing_knife;

import com.jeremyseq.inhabitants.Inhabitants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ThrowingKnifeRenderer extends GeoEntityRenderer<ThrowingKnifeProjectile> {

    public ThrowingKnifeRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ThrowingKnifeModel());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ThrowingKnifeProjectile animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/throwing_knife.png");
    }

    @Override
    protected void applyRotations(ThrowingKnifeProjectile animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        float yaw = Mth.lerp(partialTick, animatable.yRotO, animatable.getYRot());
        float pitch = Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot());

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
    }
}
