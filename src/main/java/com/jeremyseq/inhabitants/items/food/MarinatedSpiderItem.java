package com.jeremyseq.inhabitants.items.food;

import com.jeremyseq.inhabitants.effects.ModEffects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

public class MarinatedSpiderItem extends BowlFoodItem {
    public MarinatedSpiderItem() {
        super(new Item.Properties().stacksTo(1).food(
                new FoodProperties.Builder()
                .nutrition(6)
                .saturationMod(0.6f)
                .alwaysEat()
                .build()));
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(ModEffects.STICKY_LEGS.get(), 2400, 0, false, true, true));
        return super.finishUsingItem(stack, level, entity);
    }
}
