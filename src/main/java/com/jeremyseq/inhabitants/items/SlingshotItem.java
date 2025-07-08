package com.jeremyseq.inhabitants.items;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;

public class SlingshotItem extends BowItem {
    private static final Item CORRECT_PROJECTILE = Items.ARROW;

    public SlingshotItem(Properties pProperties) {
        super(pProperties);

        ItemProperties.register(this, ResourceLocation.withDefaultNamespace("pull"), (stack, world, entity, seed) -> {
            if (entity == null) return 0.0F;
            if (entity.getUseItem() != stack) return 0.0F;
            return (float)(stack.getUseDuration() - entity.getUseItemRemainingTicks()) / 20.0F;
        });

        ItemProperties.register(this, ResourceLocation.withDefaultNamespace("pulling"), (stack, world, entity, seed) -> {
            return (entity != null && entity.isUsingItem() && entity.getUseItem() == stack) ? 1.0F : 0.0F;
        });
    }

    @Override
    public void releaseUsing(ItemStack pStack, Level pLevel, LivingEntity pEntityLiving, int pTimeLeft) {
        if (!pLevel.isClientSide && pEntityLiving instanceof Player player) {
            int chargeDuration = this.getUseDuration(pStack) - pTimeLeft;
            float power = getPowerForTime(chargeDuration);

            if (power < 0.1F) return; // Not enough charge

            ItemStack ammo = findAmmo(player);
            boolean isCreative = player.isCreative();

            if (!ammo.isEmpty() || isCreative) {
                Arrow projectile = new Arrow(pLevel, pEntityLiving);
                projectile.shootFromRotation(pEntityLiving, pEntityLiving.getXRot(), pEntityLiving.getYRot(), 0.0F, 1.5F, 1.0F);
                pLevel.addFreshEntity(projectile);

                if (!isCreative) {
                    ammo.shrink(1);
                }
            }
        }
    }

    private ItemStack findAmmo(Player player) {
        if (player.getProjectile(player.getItemInHand(InteractionHand.MAIN_HAND)).is(CORRECT_PROJECTILE)) {
            return player.getProjectile(player.getItemInHand(InteractionHand.MAIN_HAND));
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(CORRECT_PROJECTILE)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public Predicate<ItemStack> getSupportedHeldProjectiles() {
        return (stack) -> stack.is(CORRECT_PROJECTILE);
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return (stack) -> stack.is(CORRECT_PROJECTILE);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack pStack) {
        return 72000;
    }

    // Mimics bow power curve
    public static float getPowerForTime(int charge) {
        float f = charge / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        return Math.min(f, 1.0F);
    }
}
