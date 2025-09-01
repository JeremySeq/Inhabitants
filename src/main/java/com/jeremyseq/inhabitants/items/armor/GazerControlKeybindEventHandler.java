package com.jeremyseq.inhabitants.items.armor;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.ModKeyBindings;
import com.jeremyseq.inhabitants.entities.gazer.GazerEntity;
import com.jeremyseq.inhabitants.items.GazerPodItem;
import com.jeremyseq.inhabitants.networking.GazerStartControlC2S;
import com.jeremyseq.inhabitants.networking.GazerStopControlC2S;
import com.jeremyseq.inhabitants.networking.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID)
public class GazerControlKeybindEventHandler {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (ModKeyBindings.CONTROL_GAZER_KEY.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;


            ItemStack helmet = mc.player.getItemBySlot(EquipmentSlot.HEAD);
            if (!(helmet.getItem() instanceof GazerPodItem)) return;

            if (GazerPodItem.hasGazer(helmet)) {
                Inhabitants.LOGGER.warn("GazerPodItem has a gazer but is being worn on the head!");
                return;
            }

            int gazerId = GazerPodItem.getGazerId(helmet);
            if (gazerId == -1 || gazerId == 0) {
                Inhabitants.LOGGER.warn("GazerPodItem has no valid gazer ID!");
                return;
            }

            if (mc.getCameraEntity() instanceof GazerEntity) {
                // already controlling → request to stop
                ModNetworking.CHANNEL.sendToServer(new GazerStopControlC2S(gazerId));
            } else {
                // not controlling → request to start
                ModNetworking.CHANNEL.sendToServer(new GazerStartControlC2S(gazerId));
            }
        }
    }
}
