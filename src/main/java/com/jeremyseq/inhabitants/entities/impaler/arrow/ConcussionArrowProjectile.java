package com.jeremyseq.inhabitants.entities.impaler.arrow;

import com.jeremyseq.inhabitants.effects.ModEffects;
import com.jeremyseq.inhabitants.entities.ModEntities;
import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

public class ConcussionArrowProjectile extends AbstractArrow {

    public ConcussionArrowProjectile(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
    }

    public ConcussionArrowProjectile(Level pLevel, LivingEntity pShooter) {
        super(ModEntities.CONCUSSION_ARROW_PROJECTILE.get(), pShooter, pLevel);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult pResult) {
        super.onHitEntity(pResult);
        if (pResult.getEntity() instanceof LivingEntity livingEntity) {
            livingEntity.addEffect(new MobEffectInstance(ModEffects.CONCUSSION.get(), 6*20));
        }
    }

    @Override
    public @NotNull ItemStack getPickupItem() {
        return ModItems.CONCUSSION_ARROW.get().getDefaultInstance();
    }
}
