package com.jeremyseq.inhabitants.items.javelin;

import com.jeremyseq.inhabitants.Inhabitants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.client.ForgeHooksClient;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

public class JavelinItemRenderer extends GeoItemRenderer<JavelinItem> {
    public JavelinItemRenderer() {
        super(new JavelinItemModel());
    }

    @Override
    public void renderByItem(
        ItemStack stack,
        ItemDisplayContext displayContext,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay) {
        
        if (displayContext == ItemDisplayContext.GUI || 
            displayContext == ItemDisplayContext.GROUND || 
            displayContext == ItemDisplayContext.FIXED) {
            
            BakedModel model = Minecraft.getInstance().getModelManager()
                .getModel(new ModelResourceLocation(Inhabitants.MODID, "javelin_gui", "inventory"));
            
            if (model != null && model != Minecraft.getInstance().getModelManager().getMissingModel()) {
                poseStack.pushPose();
                poseStack.translate(0.5f, 0.5f, 0.5f);

                BakedModel guiModel =
                    ForgeHooksClient.handleCameraTransforms(poseStack, model, displayContext, false);
                
                poseStack.translate(-0.5f, -0.5f, -0.5f);
                
                VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());
                
                int light = (displayContext == ItemDisplayContext.GUI) ?
                    LightTexture.FULL_BRIGHT : packedLight;

                Minecraft.getInstance().getItemRenderer()
                    .renderModelLists(guiModel, stack, light, packedOverlay, poseStack, consumer);
                
                poseStack.popPose();
                return;
            }
        }

        super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
    }

    @Override
    public void preRender(PoseStack poseStack,
        JavelinItem animatable,
        BakedGeoModel model,
        MultiBufferSource bufferSource,
        VertexConsumer buffer,
        boolean isReRender,
        float partialTick,
        int packedLight, int packedOverlay,
        float r, float g, float b, float a) {

        super.preRender(poseStack, animatable, model,
            bufferSource, buffer, isReRender, partialTick,
            packedLight, packedOverlay,
            r, g, b, a);
        
        if (!isReRender) {
            if (this.renderPerspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || 
                this.renderPerspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
                
                thirdPersonAiming(poseStack, partialTick);
            } else if (this.renderPerspective != null &&
                this.renderPerspective.firstPerson()) {
                
                firstPersonAiming(poseStack, partialTick);
            }
        }
    }

    private void thirdPersonAiming(PoseStack poseStack, float partialTick) {
        float xOffset = 0.0f;
        float chargeProgress = 0f;
        Player aimingPlayer = null;

        if (Minecraft.getInstance().level != null) {
            for (Player player : Minecraft.getInstance().level.players()) {
                if (isPlayerUsingThisJavelin(player)) {
                    aimingPlayer = player;
                    chargeProgress = getChargeProgress(player, partialTick);
                    break;
                }
            }
        }
        
        poseStack.translate(xOffset, -0.05f, 0.05f);
        
        if (aimingPlayer != null && chargeProgress > 0) {
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
            poseStack.translate(0, -0.1f * chargeProgress, 0);
        }
    }

    private void firstPersonAiming(PoseStack poseStack, float partialTick) {
        Player localPlayer = Minecraft.getInstance().player;
        float chargeProgress = getChargeProgress(localPlayer, partialTick);

        float xBase = 0.15f + (0.12f * Math.min(1.0f, chargeProgress * 4.0f));
        float yBase = -0.2f - (chargeProgress * 0.7f);
        float zBase = -0.25f * chargeProgress;

        poseStack.translate(xBase, yBase, zBase);
    }

    private float getChargeProgress(Player player, float partialTick) {
        if (!isPlayerUsingThisJavelin(player)) return 0f;
        return Math.min(1.0f, (player.getTicksUsingItem() + partialTick) / 60.0f);
    }

    private boolean isPlayerUsingThisJavelin(Player player) {
        return player != null && player.isUsingItem() && 
               ItemStack.isSameItemSameTags(player.getUseItem(), this.currentItemStack);
    }
}
