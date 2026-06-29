package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.entities.impaler.arrow.ConcussionArrowProjectile;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ConcussionArrowItem extends ArrowItem {
    public ConcussionArrowItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public @NotNull AbstractArrow createArrow(@NotNull Level pLevel, @NotNull ItemStack pStack, @NotNull LivingEntity pShooter) {
        return new ConcussionArrowProjectile(pLevel, pShooter);
    }
}
