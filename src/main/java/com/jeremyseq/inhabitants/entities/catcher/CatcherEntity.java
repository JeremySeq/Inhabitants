package com.jeremyseq.inhabitants.entities.catcher;

import com.jeremyseq.inhabitants.entities.goals.CooldownMeleeAttackGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
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

public class CatcherEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    private int attackAnimTimer = 0;

    private static final EntityDataAccessor<String> STATE =
            SynchedEntityData.defineId(CatcherEntity.class, EntityDataSerializers.STRING);

    public CatcherEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public enum State {
        IDLE,
        BURROWED,
        AMBUSH
    }

    public static AttributeSupplier setAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20f)
                .add(Attributes.ATTACK_DAMAGE, 5f)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5f)
                .add(Attributes.MOVEMENT_SPEED, .25f).build();
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 30f, 1));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.addBehaviourGoals();
    }

    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(1, new CatcherAmbushGoal(this));
        this.goalSelector.addGoal(2, new CatcherBurrowGoal(this));

        this.goalSelector.addGoal(6, new CooldownMeleeAttackGoal(this, 1.0D, true, 25));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void customServerAiStep() {
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
            triggerAnim("attack", "attack");
            this.attackAnimTimer = 10;
        }
        return true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, State.IDLE.name());
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putString("State", this.getState().name());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);

        if (pCompound.contains("State")) {
            this.setState(State.valueOf(pCompound.getString("State")));
        }
    }

    public State getState() {
        return State.valueOf(this.entityData.get(STATE));
    }

    public void setState(State state) {
        this.entityData.set(STATE, state.name());
    }

    public void snapToBlockCenter() {
        BlockPos pos = this.blockPosition();
        double centerX = pos.getX() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;

        this.setPos(centerX, pos.getY(), centerZ);
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "defaults_controller", 0, this::defaults));
        controllers.add(new AnimationController<>(this, "hurt", 0, state -> PlayState.STOP)
                .triggerableAnim("hurt", RawAnimation.begin().then("hurt", Animation.LoopType.PLAY_ONCE)));
        controllers.add(new AnimationController<>(this, "attack", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE)));
        controllers.add(new AnimationController<>(this, "ground_change", 0, state -> PlayState.STOP)
                .triggerableAnim("emerging", RawAnimation.begin().then("emerging", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("digging", RawAnimation.begin().then("digging", Animation.LoopType.HOLD_ON_LAST_FRAME)));
    }

    private <T extends GeoAnimatable> PlayState defaults(AnimationState<T> animationState) {
        if (animationState.isMoving()) {
            animationState.getController().setAnimation(RawAnimation.begin().then("walk", Animation.LoopType.LOOP));
        } else {
            animationState.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
