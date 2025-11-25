package com.jeremyseq.inhabitants.entities.apex;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

import java.util.Random;

public class ApexEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    private boolean randomChance = false; // used to trigger a rare idle animation

    public enum State {
        IDLE,       // awake and idling
        SLEEPING,   // sleeping
        WINDUP,     // wind-up before charging
        CHARGING,   // charging loop
        STUNNED     // stunned after hitting a block
    }

    private static final EntityDataAccessor<String> STATE =
            SynchedEntityData.defineId(ApexEntity.class, EntityDataSerializers.STRING);

    public ApexEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier setAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 400.0F)
                .add(Attributes.ATTACK_DAMAGE, 30F)
                .add(Attributes.ATTACK_SPEED, .5)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5F)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.75f)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.MOVEMENT_SPEED, .2f).build();
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 30f, 1));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.addBehaviourGoals();
    }

    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(1, new ApexSleepGoal(this));
        this.goalSelector.addGoal(2, new ApexChargeAttackGoal(this, 1.0D, 22, 40, 52));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "defaults_controller", 0, this::defaults));
        controllers.add(new AnimationController<>(this, "sleep_states", 0, state -> PlayState.STOP)
                .triggerableAnim("sleep", RawAnimation.begin().then("sleeping", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("wake", RawAnimation.begin().then("wake up", Animation.LoopType.PLAY_ONCE)));
        controllers.add(new AnimationController<>(this, "eat_bone", 0, state -> PlayState.STOP)
                .triggerableAnim("eat_bone", RawAnimation.begin().then("eating bone", Animation.LoopType.PLAY_ONCE)));
    }

    private <T extends GeoAnimatable> PlayState defaults(AnimationState<T> animationState) {
        State s = this.getState();

        if (s == State.STUNNED) {
            animationState.getController().setAnimation(RawAnimation.begin().then("stunned", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }

        if (s == State.WINDUP) {
            animationState.getController().setAnimation(RawAnimation.begin().then("running attack prepare", Animation.LoopType.PLAY_ONCE));
            return PlayState.CONTINUE;
        }

        if (s == State.CHARGING) {
            animationState.getController().setAnimation(RawAnimation.begin().then("charging", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }

        if (s == State.SLEEPING) {
            animationState.getController().setAnimation(RawAnimation.begin().then("sleeping", Animation.LoopType.LOOP));
        } else if (animationState.isMoving()) {
            animationState.getController().setAnimation(RawAnimation.begin().then("walking", Animation.LoopType.LOOP));
        } else {
            if (randomChance) {
                animationState.getController().setAnimation(RawAnimation.begin().then("Idle_rare", Animation.LoopType.PLAY_ONCE));
                if (animationState.getController().hasAnimationFinished()) {
                    animationState.getController().forceAnimationReset();
                    randomChance = false;
                    animationState.getController().forceAnimationReset();
                }
                return PlayState.CONTINUE;
            }

            animationState.getController().setAnimation(RawAnimation.begin().then("Idle", Animation.LoopType.LOOP));
            if (animationState.getController().hasAnimationFinished()) {
                animationState.getController().forceAnimationReset();
                randomChance = new Random().nextFloat() < 0.1f; // chance to trigger a rare idle animation
            }
        }
        return PlayState.CONTINUE;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, State.SLEEPING.name());
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.getState() == State.SLEEPING) {
            tag.putString("State", State.SLEEPING.name());
        } else {
            tag.putString("State", State.IDLE.name());
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("State")) {
            this.setState(ApexEntity.State.valueOf(tag.getString("State")));
        }
    }

    public State getState() {
        return State.valueOf(this.entityData.get(STATE));
    }
    public void setState(State state) {
        this.entityData.set(STATE, state.name());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
