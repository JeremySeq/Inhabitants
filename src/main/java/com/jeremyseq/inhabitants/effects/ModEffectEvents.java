package com.jeremyseq.inhabitants.effects;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
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
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Camera camera = event.getCamera();
        if (!(camera.getEntity() instanceof LivingEntity living)) return;

        FogType fogType = camera.getFluidInCamera();

        if (living.hasEffect(ModEffects.ADAPTATION.get())) {
            if (fogType == FogType.WATER || fogType == FogType.LAVA || fogType == FogType.POWDER_SNOW) {
                event.setNearPlaneDistance(0.0F);
                event.setFarPlaneDistance(128.0F);
                event.setCanceled(true);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasEffect(ModEffects.DEAFENING.get())) {
            event.setSound(null);
        }
    }
}
