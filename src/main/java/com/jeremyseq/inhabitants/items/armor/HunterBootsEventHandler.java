package com.jeremyseq.inhabitants.items.armor;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.items.HunterBootsItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID)
public class HunterBootsEventHandler {
    private static final String BOOST_TAG = "inhabitants_hunter_boots_boost";

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (!(event.getEntityMounting() instanceof LivingEntity rider)) return;
        if (!(event.getEntityBeingMounted() instanceof LivingEntity mount)) return;

        // Apply boost when mounting
        if (event.isMounting()) {
            if (rider.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof HunterBootsItem) {
                if (!mount.getPersistentData().getBoolean(BOOST_TAG)) {
                    mount.getPersistentData().putBoolean(BOOST_TAG, true);
                    mount.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 1, false, true, true));
                }
            }
        }

        // Remove boost when the rider dismounts
        if (event.isDismounting()) {
            if (mount.getPersistentData().getBoolean(BOOST_TAG)) {
                mount.removeEffect(MobEffects.MOVEMENT_SPEED);
                mount.getPersistentData().remove(BOOST_TAG);
            }
        }
    }
}
