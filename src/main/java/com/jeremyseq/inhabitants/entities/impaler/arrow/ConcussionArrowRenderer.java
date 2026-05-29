package com.jeremyseq.inhabitants.entities.impaler.arrow;

import com.jeremyseq.inhabitants.Inhabitants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ConcussionArrowRenderer extends GeoEntityRenderer<ConcussionArrowProjectile> {
    public ConcussionArrowRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new ConcussionArrowModel());
    }

    @Override
    protected void applyRotations(ConcussionArrowProjectile animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        float yaw = Mth.lerp(partialTick, animatable.yRotO, animatable.getYRot());
        float pitch = Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot());

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ConcussionArrowProjectile animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/concussion_arrow.png");
    }
}
