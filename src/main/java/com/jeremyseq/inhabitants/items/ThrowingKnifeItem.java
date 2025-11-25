package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.entities.dryfang.throwing_knife.ThrowingKnifeProjectile;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ThrowingKnifeItem extends Item {
    private final int MAX_CHARGE_TIME = 20;
    private final int COOLDOWN_TICKS = 20;

    public ThrowingKnifeItem(Properties props) {
        super(props);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    // show charging animation like a bow
    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, Level level, @NotNull LivingEntity entity, int timeLeft) {

        if (!level.isClientSide) {
            ThrowingKnifeProjectile knife = new ThrowingKnifeProjectile(level, entity);

            int timeCharged = this.getUseDuration(stack) - timeLeft;
            if (timeCharged > MAX_CHARGE_TIME) {
                timeCharged = MAX_CHARGE_TIME;
            }
            float percentCharged = (float) timeCharged / (float) MAX_CHARGE_TIME;
            knife.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), 0.0F, percentCharged*2.5F, 1.0F);

            knife.pickup = AbstractArrow.Pickup.ALLOWED;
            level.addFreshEntity(knife);
        }

        if (entity instanceof Player player) {
            if (player.getAbilities().instabuild) {
                return;
            } else {
                player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            }
        }
        stack.shrink(1);

    }
}
