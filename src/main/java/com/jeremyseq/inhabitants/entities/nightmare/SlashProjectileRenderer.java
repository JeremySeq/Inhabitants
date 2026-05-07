package com.jeremyseq.inhabitants.entities.nightmare;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

// SlashProjectile requires a renderer. This is really just a placeholder that doesn't render anything but prevents the error.
public class SlashProjectileRenderer extends EntityRenderer<SlashProjectile> {

    public SlashProjectileRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public ResourceLocation getTextureLocation(SlashProjectile slashProjectile) {
        return null;
    }

    @Override
    public boolean shouldRender(SlashProjectile pLivingEntity, Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
        return false;
    }
}
