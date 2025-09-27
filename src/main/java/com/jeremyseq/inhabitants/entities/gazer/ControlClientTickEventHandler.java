package com.jeremyseq.inhabitants.entities.gazer;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.networking.GazerControlPacketC2S;
import com.jeremyseq.inhabitants.networking.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
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

            float yaw = mc.player.getYHeadRot() % 360F;
            float pitch = mc.player.getXRot();

            // Send input packet to the server
            ModNetworking.CHANNEL.sendToServer(new GazerControlPacketC2S(gazerId, forward, back, left, right, jump, sneak, yaw, pitch));
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();

        // Check if the camera entity is a GazerEntity
        if (mc.getCameraEntity() instanceof GazerEntity) {
            event.setCanceled(true); // Cancel the hand rendering
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
