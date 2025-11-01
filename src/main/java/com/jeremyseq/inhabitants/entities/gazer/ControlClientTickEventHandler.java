package com.jeremyseq.inhabitants.entities.gazer;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.networking.GazerControlPacketC2S;
import com.jeremyseq.inhabitants.networking.ModNetworking;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID)
public class ControlClientTickEventHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Only send input if we're currently controlling a gazer
        if (mc.getCameraEntity() instanceof GazerEntity gazer) {
            boolean forward = mc.options.keyUp.isDown();
            boolean back = mc.options.keyDown.isDown();
            boolean left = mc.options.keyLeft.isDown();
            boolean right = mc.options.keyRight.isDown();
            boolean jump = mc.options.keyJump.isDown();
            boolean sneak = mc.options.keyShift.isDown();

            UUID gazerId = gazer.getUUID();

            float playerYaw = mc.player.getYHeadRot();
            float playerPitch = mc.player.getXRot();

            Inhabitants.LOGGER.debug("CLIENT: Sending Gazer control packet: forward={}, back={}, left={}, right={}, jump={}, sneak={}, yaw={}, pitch={}",
                    forward, back, left, right, jump, sneak, playerYaw, playerPitch);

            ModNetworking.CHANNEL.sendToServer(new GazerControlPacketC2S(gazerId, forward, back, left, right, jump, sneak, playerYaw, playerPitch));
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Only process if the camera entity is a GazerEntity
        if (!(mc.getCameraEntity() instanceof GazerEntity)) return;

        event.setCanceled(true); // Cancel the attack event
    }

    @SubscribeEvent
    public static void onDestroyBlock(BlockEvent.BreakEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Only process if the camera entity is a GazerEntity
        if (!(mc.getCameraEntity() instanceof GazerEntity)) return;

        event.setCanceled(true); // Cancel the block break event
    }

    @SubscribeEvent
    public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Only process if the camera entity is a GazerEntity
        if (!(mc.getCameraEntity() instanceof GazerEntity)) return;

        event.setCanceled(true); // Cancel the block placement event
    }

    @SubscribeEvent
    public static void onRenderHand(net.minecraftforge.client.event.RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();

        // Check if the camera entity is a GazerEntity
        if (mc.getCameraEntity() instanceof GazerEntity) {
            event.setCanceled(true); // Cancel the hand rendering
        }
    }

    @SubscribeEvent
    public static void onRenderGui(net.minecraftforge.client.event.RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();

        // Check if the camera entity is a GazerEntity
        if (mc.getCameraEntity() instanceof GazerEntity) {
            event.setCanceled(true); // Cancel the GUI rendering
        }
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Camera camera = event.getCamera();
        if (!(camera.getEntity() instanceof GazerEntity)) return;

        if (camera.getFluidInCamera() == FogType.LAVA) {
            event.setNearPlaneDistance(5.0F);
            event.setFarPlaneDistance(64.0F);
            event.setCanceled(true);
        }

    }

}
