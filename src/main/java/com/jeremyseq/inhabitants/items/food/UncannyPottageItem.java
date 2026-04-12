package com.jeremyseq.inhabitants.items.food;

import com.jeremyseq.inhabitants.effects.ModEffects;
import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import com.jeremyseq.inhabitants.particles.ModParticles;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

public class UncannyPottageItem extends BowlFoodItem {
    public UncannyPottageItem() {
        super(new Item.Properties().stacksTo(1).food(
                new FoodProperties.Builder()
                .nutrition(5)
                .saturationMod(0.6f)
                .alwaysEat()
                .build()));
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            boolean hasEffectAlready = entity.hasEffect(ModEffects.REVERSE_GROWTH.get());
            
            // play reverse_growth.ogg only if there was no reverse growth effect playing
            // and abracadabra particle effect only if there was no reverse growth effect playing
            if (!hasEffectAlready) {

                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                        ModSoundEvents.REVERSE_GROWTH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                
                if (level instanceof ServerLevel serverLevel) {
                    for (int i = 0; i < 20; i++) {
                        double px = entity.getX() + (level.random.nextDouble() - 0.5) * 1.0;
                        double py = entity.getY() + level.random.nextDouble() * entity.getBbHeight();
                        double pz = entity.getZ() + (level.random.nextDouble() - 0.5) * 1.0;
                        serverLevel.sendParticles(ModParticles.ABRACADABRA.get(), px, py, pz, 1, 0, 0, 0, 0.05);
                    }
                }
            }
        }
        entity.addEffect(new MobEffectInstance(ModEffects.REVERSE_GROWTH.get(), 2400, 0, false, true, true));
        return super.finishUsingItem(stack, level, entity);
    }
}