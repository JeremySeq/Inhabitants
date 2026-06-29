package com.jeremyseq.inhabitants.entities.impaler.arrow;

import com.jeremyseq.inhabitants.Inhabitants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class ConcussionArrowRenderer extends ArrowRenderer<ConcussionArrowProjectile> {
    public ConcussionArrowRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    // Copied from ArrowRenderer class and modified for longer arrow texture
    @Override
    public void render(@NotNull ConcussionArrowProjectile pEntity, float pEntityYaw, float pPartialTicks, @NotNull PoseStack pPoseStack, @NotNull MultiBufferSource pBuffer, int pPackedLight) {
        pPoseStack.pushPose();
        pPoseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(pPartialTicks, pEntity.yRotO, pEntity.getYRot()) - 90.0F));
        pPoseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTicks, pEntity.xRotO, pEntity.getXRot())));
        float uv_zero = 0.0F;
        float uv_shaft_length = 0.5625F; // 18/32 (length of shaft)
        float uv_shaft_width = 0.15625F; // 5/32 (width of arrowhead/shaft)
        float scale = 0.07F;
        float $$16 = (float)pEntity.shakeTime - pPartialTicks;
        if ($$16 > 0.0F) {
            float $$17 = -Mth.sin($$16 * 3.0F) * $$16;
            pPoseStack.mulPose(Axis.ZP.rotationDegrees($$17));
        }

        pPoseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        pPoseStack.scale(scale, scale, scale);
        pPoseStack.translate(-4.0F, 0.0F, 0.0F);
        VertexConsumer $$18 = pBuffer.getBuffer(RenderType.entityCutout(this.getTextureLocation(pEntity)));
        PoseStack.Pose $$19 = pPoseStack.last();
        Matrix4f $$20 = $$19.pose();
        Matrix3f $$21 = $$19.normal();
        this.vertex($$20, $$21, $$18, -7, -2, -2, uv_zero, uv_shaft_width, -1, 0, 0, pPackedLight);
        this.vertex($$20, $$21, $$18, -7, -2, 2, uv_shaft_width, uv_shaft_width, -1, 0, 0, pPackedLight);
        this.vertex($$20, $$21, $$18, -7, 2, 2, uv_shaft_width, 2*uv_shaft_width, -1, 0, 0, pPackedLight);
        this.vertex($$20, $$21, $$18, -7, 2, -2, uv_zero, 2*uv_shaft_width, -1, 0, 0, pPackedLight);

        this.vertex($$20, $$21, $$18, -7, 2, -2, uv_zero, uv_shaft_width, 1, 0, 0, pPackedLight);
        this.vertex($$20, $$21, $$18, -7, 2, 2, uv_shaft_width, uv_shaft_width, 1, 0, 0, pPackedLight);
        this.vertex($$20, $$21, $$18, -7, -2, 2, uv_shaft_width, 2*uv_shaft_width, 1, 0, 0, pPackedLight);
        this.vertex($$20, $$21, $$18, -7, -2, -2, uv_zero, 2*uv_shaft_width, 1, 0, 0, pPackedLight);

        for(int $$22 = 0; $$22 < 4; ++$$22) {
            pPoseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            this.vertex($$20, $$21, $$18, -8, -2, 0, uv_zero, uv_zero, 0, 1, 0, pPackedLight);
            this.vertex($$20, $$21, $$18, 8, -2, 0, uv_shaft_length, uv_zero, 0, 1, 0, pPackedLight);
            this.vertex($$20, $$21, $$18, 8, 2, 0, uv_shaft_length, uv_shaft_width, 0, 1, 0, pPackedLight);
            this.vertex($$20, $$21, $$18, -8, 2, 0, uv_zero, uv_shaft_width, 0, 1, 0, pPackedLight);
        }

        pPoseStack.popPose();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ConcussionArrowProjectile concussionArrowProjectile) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/concussion_arrow.png");
    }
}
