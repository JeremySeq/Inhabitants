package com.jeremyseq.inhabitants.entities.warped_clam;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class WarpedClamRenderer extends GeoEntityRenderer<WarpedClamEntity> {
    public WarpedClamRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new WarpedClamModel());
        addRenderLayer(new HeldItemLayer(this));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull WarpedClamEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/warped_clam.png");
    }

    private static class HeldItemLayer extends BlockAndItemGeoLayer<WarpedClamEntity> {
        public HeldItemLayer(GeoEntityRenderer<WarpedClamEntity> renderer) {
            super(renderer);
        }

        @Override
        protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, WarpedClamEntity animatable) {
            return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        }

        @Override
        protected @Nullable ItemStack getStackForBone(GeoBone bone, WarpedClamEntity animatable) {
            if ("pearl".equals(bone.getName()) && animatable.hasPearl()) {
                return new ItemStack(Items.ENDER_PEARL);
            }
            return super.getStackForBone(bone, animatable);
        }
    }
}
