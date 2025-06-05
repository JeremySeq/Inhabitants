package com.jeremyseq.inhabitants.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class FishSnotChowderItem extends Item {
    public FishSnotChowderItem() {
        super(new Item.Properties().food(
                new FoodProperties.Builder()
                        .nutrition(10)
                        .saturationMod(0.8f)
                        .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 600), 1.0f)
                        .build()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Smells awful...").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
