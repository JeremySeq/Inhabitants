package com.jeremyseq.inhabitants.entities.warped_clam;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

public class WarpedClamEntity extends Entity implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public static final EntityDataAccessor<Boolean> FLING_ANIM = SynchedEntityData.defineId(WarpedClamEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DROP_PEARL_ANIM = SynchedEntityData.defineId(WarpedClamEntity.class, EntityDataSerializers.BOOLEAN);

    public WarpedClamEntity(EntityType<? extends Entity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> animationState) {
        if (entityData.get(FLING_ANIM)) {
            animationState.getController().setAnimation(RawAnimation.begin().then("pushing", Animation.LoopType.PLAY_ONCE));
            if (animationState.getController().hasAnimationFinished()) {
                entityData.set(FLING_ANIM, false);
                animationState.getController().forceAnimationReset();
            }
            return PlayState.CONTINUE;
        }

        if (entityData.get(DROP_PEARL_ANIM)) {
            animationState.getController().setAnimation(RawAnimation.begin().then("dropping_pearl", Animation.LoopType.PLAY_ONCE));
            if (animationState.getController().hasAnimationFinished()) {
                entityData.set(DROP_PEARL_ANIM, false);
                animationState.getController().forceAnimationReset();
            }
            return PlayState.CONTINUE;
        }

        return PlayState.CONTINUE;
    }

        @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(FLING_ANIM, false);
        entityData.define(DROP_PEARL_ANIM, false);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {

    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {

    }
}
