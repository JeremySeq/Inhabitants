package com.jeremyseq.inhabitants.entities.gazer;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.networking.GazerControlPacketC2S;
import com.jeremyseq.inhabitants.networking.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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

            int gazerId = gazer.getId();

            // Send input packet to the server
            ModNetworking.CHANNEL.sendToServer(new GazerControlPacketC2S(gazerId, forward, back, left, right, jump));
        }
    }
}
