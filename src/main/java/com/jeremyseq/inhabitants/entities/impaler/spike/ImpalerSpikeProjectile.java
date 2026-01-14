package com.jeremyseq.inhabitants.entities.impaler.spike;

import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

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
}
