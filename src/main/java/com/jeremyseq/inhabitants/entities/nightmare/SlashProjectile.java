package com.jeremyseq.inhabitants.entities.nightmare;

import com.jeremyseq.inhabitants.entities.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class SlashProjectile extends AbstractHurtingProjectile {
    // use variable to store user's og position instead of checking player.pos every tick
    private static final EntityDataAccessor<Vector3f> ORIGIN = SynchedEntityData.defineId(SlashProjectile.class, EntityDataSerializers.VECTOR3);
    private static final int MAX_DISTANCE = 10;
    private static final float DAMAGE = 4.0F;
    private static final double SPEED = 1.0D;

    public SlashProjectile(EntityType<? extends AbstractHurtingProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public SlashProjectile(Level pLevel, LivingEntity pShooter) {
        super(ModEntities.SLASH_PROJECTILE.get(), pLevel);
        setOwner(pShooter);
        this.setPos(pShooter.getEyePosition());
        this.setOrigin(pShooter.getEyePosition());
        Vec3 lookVec = pShooter.getLookAngle();
        Vec3 velocity = lookVec.normalize().scale(SPEED);
        this.setDeltaMovement(velocity);
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    public boolean isPickable() {
        return false;
    }

    public boolean hurt(DamageSource pSource, float pAmount) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        // discarded after traveling 4 blocks origin
        if (distanceToSqr(this.getOrigin()) > MAX_DISTANCE * MAX_DISTANCE) {
            this.discard();
        }
        if (this.level().isClientSide && this.tickCount % 2 == 0) {
            this.level().addParticle(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult pResult) {
        super.onHitEntity(pResult);

        super.onHitEntity(pResult);
        Entity entity = this.getOwner();
        if (entity instanceof LivingEntity livingentity) {
            pResult.getEntity().hurt(this.damageSources().mobProjectile(this, livingentity), DAMAGE);
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult pResult) {
        super.onHitBlock(pResult);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ORIGIN, new Vector3f(0, 0, 0));
    }

    public void setOrigin(Vec3 origin) {
        this.entityData.set(ORIGIN, new Vector3f((float) origin.x, (float) origin.y, (float) origin.z));
    }
    public Vec3 getOrigin() {
        Vector3f vec3f = this.entityData.get(ORIGIN);
        return new Vec3(vec3f.x(), vec3f.y(), vec3f.z());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        Vec3 origin = this.getOrigin();
        if (origin != null) {
            pCompound.putDouble("originX", origin.x);
            pCompound.putDouble("originY", origin.y);
            pCompound.putDouble("originZ", origin.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        if (pCompound.contains("originX") && pCompound.contains("originY") && pCompound.contains("originZ")) {
            double x = pCompound.getDouble("originX");
            double y = pCompound.getDouble("originY");
            double z = pCompound.getDouble("originZ");
            setOrigin(new Vec3(x, y, z));
        } else {
            setOrigin(this.position());
        }
    }
}
