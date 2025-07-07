package com.jeremyseq.inhabitants.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class RawAbyssfishItem extends Item {
    public RawAbyssfishItem() {
        super(new Item.Properties().food(RAW_ABYSSFISH_FOOD));
    }

    public static final FoodProperties RAW_ABYSSFISH_FOOD = new FoodProperties.Builder()
            .nutrition(2)
            .saturationMod(0.1F)
            .meat()
            .alwaysEat()
            .build();

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack pStack, Level pLevel, @NotNull LivingEntity pLivingEntity) {
        if (!pLevel.isClientSide && pLivingEntity instanceof Player player) {
            // Add oxygen
            int maxAir = player.getMaxAirSupply(); // usually 300
            int currentAir = player.getAirSupply();
            player.setAirSupply(Math.min(currentAir + 120, maxAir));
        }
        return super.finishUsingItem(pStack, pLevel, pLivingEntity);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level world, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.literal("§bGrants extra oxygen when eaten!").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("A delicacy from the deep abyss...").withStyle(ChatFormatting.DARK_PURPLE));
    }
}