package com.jeremyseq.inhabitants.entities.bogre;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class BogreRenderer extends GeoEntityRenderer<BogreEntity> {
    private static final float scale = 1.33f;

    public BogreRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BogreModel());
        this.shadowRadius = 1.25f;
        this.withScale(scale);
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        addRenderLayer(new HeldItemLayer(this));
    }

    @Override
    public RenderType getRenderType(BogreEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull BogreEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/bogre.png");
    }

    private static class HeldItemLayer extends BlockAndItemGeoLayer<BogreEntity> {
        public HeldItemLayer(GeoEntityRenderer<BogreEntity> renderer) {
            super(renderer);
        }

        @Override
        protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, BogreEntity animatable) {
            return ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        }

        @Override
        protected @Nullable ItemStack getStackForBone(GeoBone bone, BogreEntity animatable) {
            if ("fish".equals(bone.getName()) && !animatable.getAnimateFishHeld().isEmpty()) {
                return animatable.getFishHeld();
            }

            return super.getStackForBone(bone, animatable);
        }
    }
}
