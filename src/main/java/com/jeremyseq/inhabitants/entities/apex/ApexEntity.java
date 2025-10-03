package com.jeremyseq.inhabitants.entities.apex;

import com.jeremyseq.inhabitants.entities.goals.SprintAtTargetGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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

    private int attackAnimTimer = 0;
    private boolean randomChance = false; // used to trigger a rare idle animation

    public static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(ApexEntity.class, EntityDataSerializers.BOOLEAN);

    public ApexEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier setAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 400.0F)
                .add(Attributes.ATTACK_DAMAGE, 30F)
                .add(Attributes.ATTACK_SPEED, .5)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5F)
                .add(Attributes.MOVEMENT_SPEED, .2f).build();
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 30f, 1));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.addBehaviourGoals();
    }

    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(1, new ApexSleepGoal(this));
        this.goalSelector.addGoal(5, new SprintAtTargetGoal(this, 1.4D, 7, 4));
        this.goalSelector.addGoal(6, new MeleeAttackGoal(this, 1.4D, true));
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
        controllers.add(new AnimationController<>(this, "attack", 0, state -> PlayState.STOP)
                .triggerableAnim("attack1", RawAnimation.begin().then("attack_horn", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("attack2", RawAnimation.begin().then("attack_bite", Animation.LoopType.PLAY_ONCE)));
        controllers.add(new AnimationController<>(this, "eat_bone", 0, state -> PlayState.STOP)
                .triggerableAnim("eat_bone", RawAnimation.begin().then("eating bone", Animation.LoopType.PLAY_ONCE)));
    }

    private <T extends GeoAnimatable> PlayState defaults(AnimationState<T> animationState) {
        if (this.entityData.get(SLEEPING)) {
            animationState.getController().setAnimation(RawAnimation.begin().then("sleeping", Animation.LoopType.LOOP));
        } else if (animationState.isMoving()) {
            if (this.isSprinting()) {
                animationState.getController().setAnimation(RawAnimation.begin().then("running", Animation.LoopType.LOOP));
            } else {
                animationState.getController().setAnimation(RawAnimation.begin().then("walking", Animation.LoopType.LOOP));
            }
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

            animationState.getController().setAnimation(RawAnimation.begin().then("Idle", Animation.LoopType.PLAY_ONCE));
            if (animationState.getController().hasAnimationFinished()) {
                animationState.getController().forceAnimationReset();
                randomChance = new Random().nextFloat() < 0.1f; // chance to trigger a rare idle animation
            }
        }
        return PlayState.CONTINUE;
    }

    @Override
    protected void customServerAiStep() {

        this.setSprinting(this.getTarget() != null && this.getTarget().isAlive());

        if (this.attackAnimTimer > 0) {
            this.attackAnimTimer--;
            if (this.attackAnimTimer == 0) {
                LivingEntity target = getTarget();
                if (target != null && distanceToSqr(target) <= this.getMeleeAttackRangeSqr(target)) {
                    // mob to target
                    Vec3 toTarget = target.position().subtract(this.position()).normalize();
                    // mob's facing direction (yaw)
                    Vec3 facing = Vec3.directionFromRotation(0, this.getYRot()).normalize();
                    double dot = facing.dot(toTarget);
                    // require within 60deg cone
                    if (dot > 0.5) {
                        super.doHurtTarget(target);
                    }
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        if (!level().isClientSide) {
            if (new Random().nextInt(2) == 0) {
                triggerAnim("attack", "attack1");
            } else {
                triggerAnim("attack", "attack2");
            }
            this.attackAnimTimer = 10;
        }
        return true;
    }


    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SLEEPING, true);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putBoolean("sleeping", entityData.get(SLEEPING));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("sleeping")) {
            entityData.set(SLEEPING, tag.getBoolean("sleeping"));
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
