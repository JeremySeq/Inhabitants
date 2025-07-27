package com.jeremyseq.inhabitants.overlays;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID)
public class RegisterGuiOverlays {
    @Mod.EventBusSubscriber(modid = Inhabitants.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBusEvents {
        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("bestiary_overlay", BestiaryOverlay.BESTIARY_OVERLAY);
        }
    }
}
