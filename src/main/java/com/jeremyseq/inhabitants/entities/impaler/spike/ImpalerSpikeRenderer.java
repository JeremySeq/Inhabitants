package com.jeremyseq.inhabitants.entities.impaler.spike;

import com.jeremyseq.inhabitants.Inhabitants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ImpalerSpikeRenderer extends GeoEntityRenderer<ImpalerSpikeProjectile> {

    public ImpalerSpikeRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ImpalerSpikeModel());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ImpalerSpikeProjectile animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/impaler_spike.png");
    }

    @Override
    protected void applyRotations(ImpalerSpikeProjectile animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        float yaw = Mth.lerp(partialTick, animatable.yRotO, animatable.getYRot());
        float pitch = Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot());

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw)); // match vanilla
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
    }
}
