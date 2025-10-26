package com.jeremyseq.inhabitants.entities.gazer;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.networking.GazerControlPacketC2S;
import com.jeremyseq.inhabitants.networking.ModNetworking;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderHandEvent;
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

            float playerYaw = Mth.wrapDegrees(mc.player.getYHeadRot());
            float playerPitch = Mth.wrapDegrees(mc.player.getXRot());

            // compute shortest-angle deltas (result in -180..180)
            float deltaYaw = Mth.wrapDegrees(playerYaw - gazer.getYRot());
            float deltaPitch = Mth.wrapDegrees(playerPitch - gazer.getXRot());

            // apply the shortest deltas to the gazer instead of setting absolute wrapped values
            gazer.setYRot(gazer.getYRot() + deltaYaw);
            gazer.setXRot(gazer.getXRot() + deltaPitch);
            gazer.setYHeadRot(gazer.getYRot());
            gazer.setYBodyRot(gazer.getYRot());

//            Inhabitants.LOGGER.debug("Sending Gazer control packet: forward={}, back={}, left={}, right={}, jump={}, sneak={}, yaw={}, pitch={}",
//                    forward, back, left, right, jump, sneak, yaw, pitch);

            Inhabitants.LOGGER.debug("CLIENT: Player Position: x={}, y={}, z={}", mc.player.getX(), mc.player.getY(), mc.player.getZ());

            // Send input packet to the server
            ModNetworking.CHANNEL.sendToServer(new GazerControlPacketC2S(gazerId, forward, back, left, right, jump, sneak, gazer.getYRot(), gazer.getXRot()));
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
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();

        // Check if the camera entity is a GazerEntity
        if (mc.getCameraEntity() instanceof GazerEntity) {
            event.setCanceled(true); // Cancel the hand rendering
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
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

    private static void forceChunksAround(Minecraft mc, BlockPos center, int radius) {
        if (mc.level == null) return;

        ClientChunkCache chunkCache = mc.level.getChunkSource();

        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;

                // Force the chunk to be loaded by requesting it from the server
                chunkCache.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
            }
        }
    }

    private static boolean gazerIsApproachingUnloadedChunk(Minecraft mc, GazerEntity gazer) {
        // Player position
        assert mc.player != null;
        double px = mc.player.getX();
        double pz = mc.player.getZ();

        // Gazer position
        double gx = gazer.getX();
        double gz = gazer.getZ();

        // Render distance in chunks
        int renderDistanceChunks = mc.options.renderDistance().get();
        double maxDistanceBlocks = renderDistanceChunks * 16; // 1 chunk = 16 blocks

        // Distance in XZ plane
        double dx = gx - px;
        double dz = gz - pz;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        // 90% of render distance
        return horizontalDistance >= maxDistanceBlocks * 0.9;
    }
}
