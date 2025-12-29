package com.jeremyseq.inhabitants.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FishSnotChowderItem extends BowlFoodItem {
    public FishSnotChowderItem() {
        super(new Item.Properties().food(
                new FoodProperties.Builder()
                        .nutrition(10)
                        .saturationMod(0.8f)
                        .alwaysEat()
                        .build()));
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack pStack, @NotNull Level pLevel, LivingEntity pEntityLiving) {
        if (!pEntityLiving.isInFluidType(Fluids.WATER.getFluidType())) {
            pEntityLiving.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 300, 1, false, true, true));
        }

        return super.finishUsingItem(pStack, pLevel, pEntityLiving);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.literal("Smells awful...").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
