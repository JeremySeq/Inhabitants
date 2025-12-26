package com.jeremyseq.inhabitants.items;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.Item;

public class UncannyPottageItem extends BowlFoodItem {
    public UncannyPottageItem() {
        super(new Item.Properties().food(
                new FoodProperties.Builder()
                        .nutrition(5)
                        .saturationMod(0.6f)
                        .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 300), 1.0f)
                        .build()));
    }
}
