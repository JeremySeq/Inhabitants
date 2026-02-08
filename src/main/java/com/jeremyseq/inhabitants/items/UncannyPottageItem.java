package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.effects.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.Item;

public class UncannyPottageItem extends BowlFoodItem {
    public UncannyPottageItem() {
        super(new Item.Properties().stacksTo(1).food(
                new FoodProperties.Builder()
                        .nutrition(5)
                        .saturationMod(0.6f)
                        .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 300), 1.0f)
                        .effect(() -> new MobEffectInstance(ModEffects.MONSTER_DISGUISE.get(), 600, 0), 1.0f)
                        .alwaysEat()
                        .build()));
    }
}
