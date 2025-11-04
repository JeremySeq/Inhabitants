package com.jeremyseq.inhabitants.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class Cornucopia extends Item {
    private static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(6)
            .saturationMod(0.6f)
            .alwaysEat()
            .build();

    private static final int COOLDOWN_TICKS = 20 * 60;

    public Cornucopia(Properties properties) {
        super(properties.food(FOOD));
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity) {
        if (entity instanceof Player player) {
            if (!level.isClientSide) {
                player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
                player.getFoodData().eat(FOOD.getNutrition(), FOOD.getSaturationModifier());
            }
            return stack;
        }

        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
        return 32;
    }
}
