package com.jeremyseq.inhabitants.items.javelin;

import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import com.jeremyseq.inhabitants.entities.javelin.JavelinEntity;

import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;

import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.*;
import software.bernie.geckolib.core.animation.AnimatableManager;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class JavelinItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public JavelinItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        JavelinClient.initializeJavelinClient(consumer);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack pStack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack pStack) {
        return 72000;
    }

    @Override
    public void releaseUsing(
        @NotNull ItemStack pStack,
        @NotNull Level pLevel,
        @NotNull LivingEntity pLivingEntity,
        int pTimeLeft) {
        if (pLivingEntity instanceof Player player) {
            int i = this.getUseDuration(pStack) - pTimeLeft;
            if (i >= 10) {
                if (!pLevel.isClientSide) {
                    float charge = Mth.clamp((float)i / 60.0f, 0.0f, 1.0f);

                    float velocity = 0.75f + charge * 4.5f;
                    float inaccuracy = (1.0f - charge) * 2.5f + 0.01f;

                    JavelinEntity javelin = new JavelinEntity(pLevel, player, pStack);
                    javelin.setCharge(charge);
                    
                    javelin.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, velocity, inaccuracy);
                    
                    if (player.getAbilities().instabuild) {
                        javelin.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                    }

                    pLevel.addFreshEntity(javelin);
                    pLevel.playSound(null, javelin.getX(), javelin.getY(), javelin.getZ(),
                        ModSoundEvents.JAVELIN_LAUNCHING.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                    
                    if (!player.getAbilities().instabuild) {
                        pStack.shrink(1);
                    }

                    player.getCooldowns().addCooldown(this, 8);
                }

                player.awardStat(Stats.ITEM_USED.get(this));
            }
            
            pLivingEntity.swing(pLivingEntity.getUsedItemHand());
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
        @NotNull Level pLevel,
        Player pPlayer,
        @NotNull InteractionHand pHand) {
        
        ItemStack itemstack = pPlayer.getItemInHand(pHand);

        if (itemstack.getCount() > 0) {
            pPlayer.startUsingItem(pHand);
            pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                    ModSoundEvents.JAVELIN_AIMING.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

            return InteractionResultHolder.consume(itemstack);
        } else {
            return InteractionResultHolder.fail(itemstack);
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged ||
            oldStack.getItem() != newStack.getItem();
    }
}
