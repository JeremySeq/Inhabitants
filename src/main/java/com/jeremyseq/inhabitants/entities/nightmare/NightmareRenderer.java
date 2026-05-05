package com.jeremyseq.inhabitants.entities.nightmare;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.DynamicGeoEntityRenderer;

import javax.annotation.Nullable;

public class NightmareRenderer extends DynamicGeoEntityRenderer<NightmareEntity> {
    private static final ResourceLocation OUTLINE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/nightmare/nightmare_head_outline.png");
    private static final ResourceLocation GLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/nightmare/nightmare_head_glow.png");


    public NightmareRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NightmareModel());
    }

    @Nullable
    @Override
    protected ResourceLocation getTextureOverrideForBone(GeoBone bone, NightmareEntity animatable, float partialTick) {
        if (bone.getName().equals("headOutline")) {
            return OUTLINE_TEXTURE;
        } else if (bone.getName().equals("headGlow")) {
            return GLOW_TEXTURE;
        }
        return null;
    }

    @Override
    protected @org.jetbrains.annotations.Nullable RenderType getRenderTypeOverrideForBone(GeoBone bone, NightmareEntity animatable, ResourceLocation texturePath, MultiBufferSource bufferSource, float partialTick) {
        if (bone.getName().equals("headOutline")) {
            return RenderType.entityTranslucentCull(texturePath);
        } else if (bone.getName().equals("headGlow")) {
            return RenderType.entityTranslucentCull(texturePath);
        }

        return super.getRenderTypeOverrideForBone(bone, animatable, texturePath, bufferSource, partialTick);
    }
}
