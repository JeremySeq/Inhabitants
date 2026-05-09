package com.jeremyseq.inhabitants.entities.impaler.spike;

import com.jeremyseq.inhabitants.effects.ModEffects;
import com.jeremyseq.inhabitants.items.ModItems;
import com.jeremyseq.inhabitants.damagesource.ModDamageTypes;
import com.jeremyseq.inhabitants.entities.impaler.ImpalerEntity;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.*;
import software.bernie.geckolib.core.animation.AnimatableManager;

import org.jetbrains.annotations.NotNull;

public class ImpalerSpikeProjectile extends AbstractArrow implements GeoAnimatable {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public ImpalerSpikeProjectile(EntityType<? extends ImpalerSpikeProjectile> type, Level level) {
        super(type, level);

        this.setBaseDamage(4.0D);
        this.setKnockback(3);
        this.setCritArrow(true);
    }

    public ImpalerSpikeProjectile(EntityType<? extends AbstractArrow> type, LivingEntity shooter, Level world) {
        super(type, shooter, world);

        this.setBaseDamage(4.0D);
        this.setKnockback(3);
        this.setCritArrow(true);
    }

    @Override
    protected @NotNull ItemStack getPickupItem() {
        return new ItemStack(ModItems.IMPALER_SPIKE.get());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object o) {
        return 0;
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 motion = this.getDeltaMovement();
        double dx = motion.x;
        double dy = motion.y;
        double dz = motion.z;

        float horizontalMag = Mth.sqrt((float)(dx * dx + dz * dz));

        this.setYRot((float)(Mth.atan2(dx, dz) * (180F / Math.PI)));
        this.setXRot((float)(Mth.atan2(dy, horizontalMag) * (180F / Math.PI)));

        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        Entity entity = pResult.getEntity();
        
        if (entity instanceof ImpalerEntity) return;

        float f = (float)this.getDeltaMovement().length();
        int i = Mth.ceil(Mth.clamp((double)f * this.getBaseDamage(), 0.0D, Integer.MAX_VALUE));

        if (this.isCritArrow()) {
            i += this.random.nextInt(i / 2 + 2);
        }

        Entity shooter = this.getOwner();
        DamageSource damagesource =
            ModDamageTypes.causeImpaledDamage(this.level(), this, shooter);

        if (entity instanceof Player player) {
            player.addEffect(new MobEffectInstance(ModEffects.CONCUSSION.get(), 100, 0));
        }

        if (entity instanceof LivingEntity livingEntity) {
            if (this.getKnockback() > 0) {
                double d0 = Math.max(0.0D, 1.0D - livingEntity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                Vec3 vec3 = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).normalize().scale((double) this.getKnockback() * 0.6D * d0);
                if (vec3.lengthSqr() > 0.0D) {
                    entity.push(vec3.x, 0.1D, vec3.z);
                }
            }

            if (entity.hurt(damagesource, (float) i)) {
                this.discard();
            } else {
                this.setDeltaMovement(this.getDeltaMovement().scale(-0.1D));
                this.setYRot(this.getYRot() + 180.0F);
                this.yRotO += 180.0F;
            }
        }
    }
}
