package com.jeremyseq.inhabitants.entities.nightmare.slash_projectile;

import com.jeremyseq.inhabitants.entities.ModEntities;
import com.jeremyseq.inhabitants.particles.ModParticles;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class SlashProjectile extends AbstractHurtingProjectile {
    // use variable to store user's og position instead of checking player.pos every tick
    private static final EntityDataAccessor<Vector3f> ORIGIN = SynchedEntityData.defineId(SlashProjectile.class, EntityDataSerializers.VECTOR3);
    public static final int MAX_DISTANCE = 14;
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
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    public boolean isPickable() {
        return false;
    }

    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        // discarded after traveling 4 blocks origin
        if (distanceToSqr(this.getOrigin()) > MAX_DISTANCE * MAX_DISTANCE) {
            this.discard();
        }

        if (this.getDeltaMovement().lengthSqr() >= 0.0001) {
            this.setDeltaMovement(this.getDeltaMovement().normalize().scale(SPEED));
        }

        // expand hitbox as particle gets bigger but move it back so it stays centered on the slash (leave a little extra space in front tho)
        this.setBoundingBox(this.getBoundingBox().inflate(this.tickCount * 0.1, 0, this.tickCount * 0.1));
        this.setBoundingBox(this.getBoundingBox().move(-this.getDeltaMovement().x * 0.09, 0, -this.getDeltaMovement().z * 0.09));

        if (this.level().isClientSide && this.tickCount % 3 == 0) {
            double yaw = Math.atan2(this.getDeltaMovement().z, this.getDeltaMovement().x);
            double pitch = Math.atan2(this.getDeltaMovement().y, SPEED);
            // scale up particle size over time
            this.level().addParticle(ModParticles.RIFTBLADE_SLASH.get(), this.getX(), this.getY(), this.getZ(), pitch, yaw, Math.pow(this.tickCount / 2.0, 1.3));
        }
    }

    @Override
    protected void onHit(HitResult pResult) {
        HitResult.Type hitresult$type = pResult.getType();
        if (hitresult$type == HitResult.Type.ENTITY) {
            this.onHitEntity((EntityHitResult)pResult);
        } else if (hitresult$type == HitResult.Type.BLOCK) {
            BlockHitResult blockhitresult = (BlockHitResult)pResult;
            this.onHitBlock(blockhitresult);
            BlockPos blockpos = blockhitresult.getBlockPos();
            this.level().gameEvent(GameEvent.PROJECTILE_LAND, blockpos, GameEvent.Context.of(this, this.level().getBlockState(blockpos)));
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult pResult) {
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
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
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
