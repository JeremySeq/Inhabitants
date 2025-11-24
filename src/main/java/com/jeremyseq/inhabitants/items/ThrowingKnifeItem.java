package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.entities.dryfang.throwing_knife.ThrowingKnifeProjectile;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ThrowingKnifeItem extends Item {
    public ThrowingKnifeItem(Properties props) {
        super(props);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            ThrowingKnifeProjectile knife = new ThrowingKnifeProjectile(level, player);
            knife.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, 1.0F);

            knife.pickup = AbstractArrow.Pickup.ALLOWED;
            level.addFreshEntity(knife);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
