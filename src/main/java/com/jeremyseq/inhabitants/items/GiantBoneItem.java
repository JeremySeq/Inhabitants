package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.entities.bogre.ai.ShockwaveGoal;
import com.jeremyseq.inhabitants.networking.bogre.ShockwaveParticlePacketS2C;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import org.jetbrains.annotations.NotNull;

public class GiantBoneItem extends SwordItem {
    public GiantBoneItem() {
        super(Tiers.NETHERITE, 7, -3.5f, new Item.Properties().stacksTo(1));
    }

    private static final float SHOCKWAVE_RADIUS = 9;
    private static final float SHOCKWAVE_DAMAGE = 20f;

    @Override
    public void inventoryTick(@NotNull ItemStack stack, Level level, @NotNull Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        if (selected && !player.hasEffect(MobEffects.DAMAGE_BOOST)) {
            MobEffectInstance current = player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (current == null || current.getAmplifier() < 1 || current.getDuration() <= 10) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, true, false, true));
            }
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level pLevel, @NotNull Player pPlayer, @NotNull InteractionHand pUsedHand) {

        HitResult rayTraceResult = pPlayer.pick(pPlayer.getBlockReach(), 0, true);
        if (rayTraceResult.getType() == HitResult.Type.MISS) {
            return super.use(pLevel, pPlayer, pUsedHand);
        }

        if (!pLevel.isClientSide) {
            ShockwaveGoal.addShockwave((ServerLevel) pLevel, rayTraceResult.getLocation(), SHOCKWAVE_DAMAGE, SHOCKWAVE_RADIUS, 40, pPlayer);
        }

        pPlayer.getCooldowns().addCooldown(this, 100);

        return super.use(pLevel, pPlayer, pUsedHand);
    }

    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return true;
    }
}
