package com.jeremyseq.inhabitants.items;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

public class CookedAbyssfishItem extends Item {
    public CookedAbyssfishItem() {
        super(new Item.Properties().food(COOKED_ABYSSFISH_FOOD));
    }

    public static final FoodProperties COOKED_ABYSSFISH_FOOD = new FoodProperties.Builder()
            .nutrition(6)
            .saturationMod(0.8F)
            .meat()
            .build();
}