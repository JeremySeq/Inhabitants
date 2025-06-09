package com.jeremyseq.inhabitants.items;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

public class RawAbyssfishItem extends Item {
    public RawAbyssfishItem() {
        super(new Item.Properties().food(RAW_ABYSSFISH_FOOD));
    }

    public static final FoodProperties RAW_ABYSSFISH_FOOD = new FoodProperties.Builder()
            .nutrition(2)
            .saturationMod(0.1F)
            .meat()
            .build();
}