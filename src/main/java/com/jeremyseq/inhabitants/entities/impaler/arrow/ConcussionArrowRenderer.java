package com.jeremyseq.inhabitants.entities.impaler.arrow;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ConcussionArrowRenderer extends ArrowRenderer<ConcussionArrowProjectile> {
    public ConcussionArrowRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(@NotNull ConcussionArrowProjectile pEntity, float pEntityYaw, float pPartialTicks, @NotNull PoseStack pPoseStack, @NotNull MultiBufferSource pBuffer, int pPackedLight) {

        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ConcussionArrowProjectile concussionArrowProjectile) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/projectiles/spectral_arrow.png");
    }
}
