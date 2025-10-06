package com.jeremyseq.inhabitants.effects;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID)
public class ModEffectEvents {
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.getItemBySlot(EquipmentSlot.CHEST).getItem() == ModItems.CHITIN_CHESTPLATE.get()) {
            entity.addEffect(new MobEffectInstance(ModEffects.IMMUNITY.get(), 30, 0, false, false, true));
        }

        if (entity.hasEffect(ModEffects.IMMUNITY.get())) {
            for (MobEffectInstance effect : new ArrayList<>(entity.getActiveEffects())) {
                if (!effect.getEffect().equals(ModEffects.IMMUNITY.get())) {
                    entity.removeEffect(effect.getEffect());
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasEffect(ModEffects.CONCUSSION.get())) {
            event.setSound(null);
        }
    }
}
