package com.jeremyseq.inhabitants.items;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ApexHorn extends Item {
    public ApexHorn(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level pLevel, @NotNull Player pPlayer, @NotNull InteractionHand pUsedHand) {
        if (!pLevel.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) pLevel;

            // current weather
            boolean isClear = !serverLevel.isRaining() && !serverLevel.isThundering();
            boolean isRain = serverLevel.isRaining() && !serverLevel.isThundering();
            boolean isThunder = serverLevel.isThundering();

            int duration = 20 * 60 * 2;

            // cycle: clear, rain, thunder
            if (isClear) {
                serverLevel.setWeatherParameters(0, 0, true, false);
            } else if (isRain) {
                serverLevel.setWeatherParameters(0, duration, true, true);
            } else if (isThunder) {
                serverLevel.setWeatherParameters(0, duration, false, false);
            }
        }

        pPlayer.getCooldowns().addCooldown(pPlayer.getItemInHand(pUsedHand).getItem(), 20 * 10);

        return InteractionResultHolder.sidedSuccess(pPlayer.getItemInHand(pUsedHand), pLevel.isClientSide());
    }
}
