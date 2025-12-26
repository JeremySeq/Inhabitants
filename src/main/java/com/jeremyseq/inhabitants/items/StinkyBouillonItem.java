package com.jeremyseq.inhabitants.items;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class StinkyBouillonItem extends BowlFoodItem {
    public StinkyBouillonItem() {
        super(new Item.Properties().food(
                new FoodProperties.Builder()
                        .nutrition(6)
                        .saturationMod(0.6f)
                        .build()));
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack pStack, @NotNull Level pLevel, LivingEntity pEntityLiving) {
        if (pEntityLiving.getActiveEffects().isEmpty()) {
            pEntityLiving.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1, false, true, true));
            return super.finishUsingItem(pStack, pLevel, pEntityLiving);
        }

        int removedCount = pEntityLiving.getActiveEffects().size();

        pEntityLiving.removeAllEffects();

        if (removedCount > 0 && pEntityLiving instanceof Player player) {
            player.getFoodData().eat(removedCount * 2, removedCount * 0.1f);
        }

        return super.finishUsingItem(pStack, pLevel, pEntityLiving);
    }
}
