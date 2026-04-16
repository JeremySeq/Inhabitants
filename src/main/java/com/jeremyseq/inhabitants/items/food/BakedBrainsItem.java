package com.jeremyseq.inhabitants.items.food;

import com.jeremyseq.inhabitants.effects.ModEffects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

public class BakedBrainsItem extends Item {
    public BakedBrainsItem() {
        super(new Item.Properties().stacksTo(1).food(
                new FoodProperties.Builder()
                .nutrition(4)
                .saturationMod(0.3f)
                .effect(() -> new MobEffectInstance(ModEffects.UNDEAD_DISGUISE.get(), 1800, 0), 1.0f)
                .alwaysEat()
                .build()));
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity) {
        super.finishUsingItem(stack, level, entity);
        
        if (!(entity instanceof Player player && player.getAbilities().instabuild)) {
            return new ItemStack(Items.SKELETON_SKULL);
        }
        
        return stack;
    }
}
