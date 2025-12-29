package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.effects.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UncannyPottageItem extends BowlFoodItem {
    public UncannyPottageItem() {
        super(new Item.Properties().food(
                new FoodProperties.Builder()
                        .nutrition(5)
                        .saturationMod(0.6f)
                        .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 300), 1.0f)
                        .effect(() -> new MobEffectInstance(ModEffects.ROTTING_DISGUISE.get(), 600, 0), 1.0f)
                        .alwaysEat()
                        .build()));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.literal("Smells like a rotting corpse!").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
