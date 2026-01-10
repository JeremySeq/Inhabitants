package com.jeremyseq.inhabitants.entities.bogre;

import com.jeremyseq.inhabitants.Inhabitants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class BogreRenderer extends GeoEntityRenderer<BogreEntity> {
    private static final float scale = 1.25f;

    private int lastSpitTick = -1;

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
    public void postRender(PoseStack poseStack, BogreEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        // spit particles

        if (!animatable.level().isClientSide) return;
        if (!animatable.isRoaring()) return;

        int tick = animatable.getRoaringTick();
        if (tick >= 10 && tick <= 12 && lastSpitTick != tick) {

            GeoBone head = this.getGeoModel().getBone("head").orElse(null);
            if (head == null) return;

            Vec3 mouthPos = new Vec3(head.getWorldPosition().x, head.getWorldPosition().y, head.getWorldPosition().z);
            Vec3 look = animatable.getLookAngle().normalize();
            mouthPos = mouthPos.add(look.x * 0.2, look.y * 0.2, look.z * 0.2);
            ClientLevel level = (ClientLevel) animatable.level();

            double vx = look.x * 0.6 + level.random.nextGaussian() * 0.05;
            double vy = (look.y-.4) * .3 + level.random.nextGaussian() * 0.05; // more downward bias
            double vz = look.z * 0.6 + level.random.nextGaussian() * 0.05;

            level.addParticle(
                    ParticleTypes.POOF,
                    mouthPos.x,
                    mouthPos.y,
                    mouthPos.z,
                    vx,
                    vy,
                    vz
            );
        }
        lastSpitTick = tick;

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
            if ("fish".equals(bone.getName()) && !animatable.getAnimateItemHeld().isEmpty()) {
                return animatable.getItemHeld();
            }

            return super.getStackForBone(bone, animatable);
        }
    }
}
