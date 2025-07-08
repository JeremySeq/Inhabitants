package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.items.tickers.LassoTicker;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class LassoItem extends Item {
    public LassoItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (!pLevel.isClientSide) {
            Entity target = raycastTarget(pPlayer, 100);

            if (target instanceof Mob mob) {
                LassoTicker.startLassoing(pPlayer, mob);
                pPlayer.getCooldowns().addCooldown(this, 20);
                return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
            }
        }

        return super.use(pLevel, pPlayer, pUsedHand);
    }

    private Entity raycastTarget(Entity user, double range) {
        Vec3 eyePos = user.getEyePosition(1.0F);
        Vec3 lookVec = user.getViewVector(1.0F);
        Vec3 reachVec = eyePos.add(lookVec.scale(range));
        AABB box = user.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0);

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                user, eyePos, reachVec, box,
                (e) -> e instanceof Mob && e.isAlive() && !e.isSpectator(),
                range * range
        );

        if (entityHit != null) {
            return entityHit.getEntity();
        }
        return null;
    }

}
