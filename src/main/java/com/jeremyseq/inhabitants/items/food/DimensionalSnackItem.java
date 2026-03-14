package com.jeremyseq.inhabitants.items.food;

import com.jeremyseq.inhabitants.util.PlayerPositionTracker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DimensionalSnackItem extends BowlFoodItem {
    public DimensionalSnackItem() {
        super(new Item.Properties().stacksTo(1).food(
            new FoodProperties.Builder()
            .nutrition(2)
            .saturationMod(0.2f)
            .alwaysEat()
            .build()));
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            PlayerPositionTracker.PositionRecord record = PlayerPositionTracker.getPastPosition(player);
            if (record != null) {
                ServerLevel targetLevel = Objects.requireNonNull(player.getServer()).getLevel(record.dimension());
                if (targetLevel != null) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                    
                    player.teleportTo(targetLevel, record.pos().x, record.pos().y, record.pos().z,
                    record.yRot(), record.xRot());
                    
                    targetLevel.playSound(null, record.pos().x, record.pos().y, record.pos().z,
                    SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
