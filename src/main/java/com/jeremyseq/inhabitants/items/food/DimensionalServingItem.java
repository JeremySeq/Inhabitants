package com.jeremyseq.inhabitants.items.food;

import com.jeremyseq.inhabitants.effects.ModEffects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffectInstance;

import org.jetbrains.annotations.NotNull;

public class DimensionalServingItem extends BowlFoodItem {
    public DimensionalServingItem() {
        super(new Item.Properties().stacksTo(1).food(
            new FoodProperties.Builder()
            .nutrition(2)
            .saturationMod(0.2f)
            .alwaysEat()
            .build()));
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity) {
        if (!level.isClientSide) {
            entity.addEffect(new MobEffectInstance(ModEffects.IMMATERIAL.get(), 2400));
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
