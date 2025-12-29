package com.jeremyseq.inhabitants.effects;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID)
public class ModEffectEvents {
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        // chitin armor immunity effect
        if (entity.getItemBySlot(EquipmentSlot.CHEST).getItem() == ModItems.CHITIN_CHESTPLATE.get() || entity.getItemBySlot(EquipmentSlot.CHEST).getItem() == ModItems.CHITIN_CHESTPLATE_ELYTRA.get()) {
            entity.addEffect(new MobEffectInstance(ModEffects.IMMUNITY.get(), 30, 0, false, false, true));
        }

        // immunity effect logic
        if (entity.hasEffect(ModEffects.IMMUNITY.get())) {
            for (MobEffectInstance effect : new ArrayList<>(entity.getActiveEffects())) {
                if (!effect.getEffect().equals(ModEffects.IMMUNITY.get())) {
                    entity.removeEffect(effect.getEffect());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();

        // immunity effect instant damage negation
        if (entity.hasEffect(ModEffects.IMMUNITY.get())) {
            if (event.getSource().type().equals(DamageTypes.MAGIC) || event.getSource().type().equals(DamageTypes.WITHER) || event.getSource().type().equals(DamageTypes.DRAGON_BREATH) || event.getSource().is(DamageTypes.INDIRECT_MAGIC)) {
                event.setCanceled(true);
            }
        }
    }

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
