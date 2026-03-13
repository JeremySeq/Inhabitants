package com.jeremyseq.inhabitants.items.food;

import com.jeremyseq.inhabitants.effects.ModEffects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.Item;

public class MonsterMealItem extends BowlFoodItem {
    public MonsterMealItem() {
        super(new Item.Properties().stacksTo(1).food(
                new FoodProperties.Builder()
                        .nutrition(4)
                        .saturationMod(0.3f)
                        .effect(() -> new MobEffectInstance(ModEffects.MONSTER_DISGUISE.get(), 2400, 0), 1.0f)
                        .alwaysEat()
                        .build()));
    }
}
