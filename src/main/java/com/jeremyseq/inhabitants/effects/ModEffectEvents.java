package com.jeremyseq.inhabitants.effects;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID)
public class ModEffectEvents {
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        Minecraft mc = Minecraft.getInstance();
        // concussion effect
        if (mc.player != null && mc.player.hasEffect(ModEffects.CONCUSSION.get())) {
            event.setSound(null);
        }
    }
}
