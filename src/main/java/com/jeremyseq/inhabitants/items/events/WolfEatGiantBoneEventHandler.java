package com.jeremyseq.inhabitants.items.events;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID)
public class WolfEatGiantBoneEventHandler {
    private static final Map<UUID, BoneTarget> wolfBoneDelays = new HashMap<>();
    private static final int EAT_DELAY_TICKS = 20; // 1 second

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf) || wolf.level().isClientSide || !wolf.isAlive())
            return;

        UUID wolfId = wolf.getUUID();
        Level level = wolf.level();

        // Clean up expired entries
        wolfBoneDelays.entrySet().removeIf(entry -> level.getEntity(entry.getValue().bone.getId()) == null);

        BoneTarget current = wolfBoneDelays.get(wolfId);

        if (current != null && current.bone.isAlive() && wolf.distanceToSqr(current.bone) <= 3.0) {
            // Near bone, decrement delay
            current.ticksRemaining--;

            if (current.ticksRemaining <= 0) {
                // Eat the bone
                current.bone.discard();

                wolf.heal(wolf.getMaxHealth() - wolf.getHealth());
                wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2400, 1, true, true, true));
                wolf.playSound(SoundEvents.WOLF_GROWL, 1.0F, 1.0F);
                wolf.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);

                wolfBoneDelays.remove(wolfId);
            }
        } else {
            // Check for nearby bone
            List<ItemEntity> bones = level.getEntitiesOfClass(
                    ItemEntity.class,
                    wolf.getBoundingBox().inflate(1.5),
                    item -> item.getItem().getItem() == ModItems.GIANT_BONE.get() && item.isAlive()
            );

            if (!bones.isEmpty()) {
                wolfBoneDelays.put(wolfId, new BoneTarget(bones.get(0), EAT_DELAY_TICKS));
            } else {
                wolfBoneDelays.remove(wolfId);
            }
        }
    }

    // Helper record for tracking bone target
    private static class BoneTarget {
        public final ItemEntity bone;
        public int ticksRemaining;

        public BoneTarget(ItemEntity bone, int delay) {
            this.bone = bone;
            this.ticksRemaining = delay;
        }
    }
}