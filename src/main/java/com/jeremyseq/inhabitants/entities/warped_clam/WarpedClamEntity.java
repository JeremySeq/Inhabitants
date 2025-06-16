package com.jeremyseq.inhabitants.entities.warped_clam;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

import java.util.List;

public class WarpedClamEntity extends Entity implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public static final EntityDataAccessor<Boolean> OPEN = SynchedEntityData.defineId(WarpedClamEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> FLING_ANIM = SynchedEntityData.defineId(WarpedClamEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> HAS_PEARL = SynchedEntityData.defineId(WarpedClamEntity.class, EntityDataSerializers.BOOLEAN);

    private int pearlRegenTimer = 0; // Ticks until pearl regenerates
    private int launchDelayTicks = 0;
    private int popDelayTicks = 0;
    private boolean lastOpenState = false;

    public WarpedClamEntity(EntityType<? extends Entity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();

        // regen pearl
        if (!hasPearl() && pearlRegenTimer > 0) {
            pearlRegenTimer--;
            if (pearlRegenTimer <= 0) {
                setHasPearl(true);
            }
        }

        // Check for players standing on top and trigger launch delay
        if (!level().isClientSide && launchDelayTicks == 0) {
            AABB topBox = getBoundingBox().move(0, 0.4, 0); // slightly above the clam
            List<LivingEntity> entities = level().getEntitiesOfClass(LivingEntity.class, topBox);

            if (!entities.isEmpty()) {
                entityData.set(FLING_ANIM, false);
                entityData.set(FLING_ANIM, true);
                launchDelayTicks = 8;
            }
        }

        // Handle launch delay
        if (launchDelayTicks > 0) {
            launchDelayTicks--;
            if (launchDelayTicks == 0) {
                launchEntity();
            }
        }

        // handle pearl pop delay
        if (popDelayTicks > 0) {
            popDelayTicks--;
            if (popDelayTicks == 0) {
                entityData.set(OPEN, false);
            }
        }

        // particles if clam has pearl
        if (hasPearl() && level().isClientSide && level().random.nextFloat() < 0.1f) {
            for (int i = 0; i < 8; i++) {
                double x = getX() + (random.nextDouble() - 0.5);
                double y = getY() + 0.7 + random.nextDouble() * 0.4;
                double z = getZ() + (random.nextDouble() - 0.5);

                double dx = (random.nextDouble() - 0.5) * 0.02;
                double dy = random.nextDouble() * 0.01;
                double dz = (random.nextDouble() - 0.5) * 0.02;

                level().addParticle(ParticleTypes.PORTAL, x, y, z, dx, dy, dz);
            }
        }
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        ItemStack item = player.getItemInHand(hand);
        if (!isOpen() && item.is(Items.BRUSH)) {
            if (!level().isClientSide) {
                entityData.set(OPEN, true);
                popDelayTicks = 60; // how long clam stays open

                level().playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_CHEST_OPEN, SoundSource.NEUTRAL, 3.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        } else if (hasPearl() && isOpen()) {
            popPearl();
            entityData.set(OPEN, false);
            entityData.set(HAS_PEARL, false);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        return super.interact(player, hand);
    }


    private void launchEntity() {
        AABB box = getBoundingBox().move(0, 0.4, 0);
        List<LivingEntity> entities = level().getEntitiesOfClass(LivingEntity.class, box);
        for (LivingEntity entity : entities) {
            if (entity.onGround()) {
                // Get direction clam is facing
                float yaw = this.getYRot(); // Degrees
                double xDir = -Math.sin(Math.toRadians(yaw));
                double zDir = Math.cos(Math.toRadians(yaw));
                Vec3 backward = new Vec3(xDir, 0, zDir).scale(-2);

                Vec3 launchVec = backward.add(0, 2, 0);

                entity.setDeltaMovement(launchVec);
                entity.hurtMarked = true;

                level().playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 4.0f, 1.0f);
            }
        }
    }

    private void popPearl() {
        ItemEntity pearl = new ItemEntity(level(), getX(), getY() + 1, getZ(), new ItemStack(Items.ENDER_PEARL));
        level().addFreshEntity(pearl);
        consumePearl();
    }

    private void consumePearl() {
        pearlRegenTimer = 20 * (120 + random.nextInt(60)); // 2-3 mins
        setHasPearl(false);
    }

    public boolean hasPearl() {
        return this.entityData.get(HAS_PEARL);
    }

    public boolean isOpen() {
        return this.entityData.get(OPEN);
    }

    public void setHasPearl(boolean value) {
        this.entityData.set(HAS_PEARL, value);
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> animationState) {
        AnimationController<?> controller = animationState.getController();

        if (entityData.get(FLING_ANIM)) {
            controller.setAnimation(RawAnimation.begin().then("pushing", Animation.LoopType.PLAY_ONCE));

            if (controller.hasAnimationFinished()) {
                entityData.set(FLING_ANIM, false);
                controller.forceAnimationReset();
            }

            return PlayState.CONTINUE;
        }

        boolean isOpen = entityData.get(OPEN);

        if (isOpen) {
            controller.setAnimation(RawAnimation.begin().then("open", Animation.LoopType.HOLD_ON_LAST_FRAME));
        } else {
            // only play close once when transitioning from open to closed
            if (lastOpenState) {
                controller.setAnimation(RawAnimation.begin().then("close", Animation.LoopType.HOLD_ON_LAST_FRAME));
            }
        }

        lastOpenState = isOpen;
        return PlayState.CONTINUE;
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(FLING_ANIM, false);
        entityData.define(HAS_PEARL, true);
        entityData.define(OPEN, false);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {

    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {

    }
}
