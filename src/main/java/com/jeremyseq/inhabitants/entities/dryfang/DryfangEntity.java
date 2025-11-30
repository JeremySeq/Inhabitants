package com.jeremyseq.inhabitants.entities.dryfang;

import com.jeremyseq.inhabitants.entities.goals.CooldownMeleeAttackGoal;
import com.jeremyseq.inhabitants.entities.goals.SprintAtTargetGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

import java.util.List;
import java.util.OptionalInt;

public class DryfangEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    private static final EntityDataAccessor<Boolean> ANGRY =
            SynchedEntityData.defineId(DryfangEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> PACK_LEADER_ID =
            SynchedEntityData.defineId(DryfangEntity.class, EntityDataSerializers.INT);

    private static final double PACK_RADIUS = 16.0D;
    private static final int PACK_MIN = 3;
    private static final int PACK_MAX = 6;

    private int attackAnimTimer = 0;

    public DryfangEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier setAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0F)
                .add(Attributes.ATTACK_DAMAGE, 5F)
                .add(Attributes.ATTACK_SPEED, 1)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0F)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.25f)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2f).build();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(5, new SprintAtTargetGoal(this, 1.4D, 7, 4));
        this.goalSelector.addGoal(6, new CooldownMeleeAttackGoal(this, 1.4D, true, 15));

        this.goalSelector.addGoal(7, new FollowPackGoal(this, 1.0D, 2.0D, 4.0D));

        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));

        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 30f, 1));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new MeatyPlayerTargetGoal(this, false));
    }

    @Override
    public void setTarget(@Nullable LivingEntity pTarget) {
        if (pTarget != null) {
            this.playSound(SoundEvents.WOLF_GROWL, 1.0F, 1.0F);
            this.setAngry(true);
        } else {
            this.setAngry(false);
        }

        super.setTarget(pTarget);
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

        // pack formation: server side only
        if (!this.level().isClientSide) {
            if (getPackLeaderEntity() == null) {
                List<DryfangEntity> nearby = this.level().getEntitiesOfClass(
                        DryfangEntity.class,
                        this.getBoundingBox().inflate(PACK_RADIUS),
                        e -> true
                );

                int total = nearby.size();
                if (total >= PACK_MIN && total <= PACK_MAX) {
                    // pack leader: lowest entity id
                    OptionalInt minIdOpt = nearby.stream().mapToInt(Entity::getId).min();
                    int leaderId = minIdOpt.getAsInt();
                    for (DryfangEntity d : nearby) {
                        d.setPackLeaderId(leaderId);
                    }
                }
            } else {
                // clear leader if leader removed or too far
                Entity leader = getPackLeaderEntity();
                if (leader == null || leader.isRemoved() || leader.distanceToSqr(this) > PACK_RADIUS * PACK_RADIUS) {
                    setPackLeaderId(-1);
                }
            }
        }
    }

    // Pack leader accessor helpers
    public int getPackLeaderId() {
        return this.entityData.get(PACK_LEADER_ID);
    }

    public void setPackLeaderId(int id) {
        this.entityData.set(PACK_LEADER_ID, id);
    }

    public boolean isLeader() {
        return getPackLeaderId() == this.getId();
    }

    @Nullable
    public Entity getPackLeaderEntity() {
        int id = getPackLeaderId();
        if (id < 0) {
            return null;
        } else {
            this.level();
        }
        Entity e = this.level().getEntity(id);
        return e instanceof DryfangEntity ? e : null;
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        if (!level().isClientSide) {
            triggerAnim("attack", "attack");
            this.attackAnimTimer = 5;
        }
        this.playSound(SoundEvents.WOLF_PANT, 1.0F, 1.0F);
        return true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANGRY, false);
        this.entityData.define(PACK_LEADER_ID, -1);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("PackLeaderId", getPackLeaderId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        setPackLeaderId(pCompound.getInt("PackLeaderId"));
    }

    public boolean isAngry() {
        return this.entityData.get(ANGRY);
    }

    public void setAngry(boolean value) {
        this.entityData.set(ANGRY, value);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "defaults_controller", 0, this::defaults));
        controllers.add(new AnimationController<>(this, "attack", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE)));
    }

    private <T extends GeoAnimatable> PlayState defaults(AnimationState<T> animationState) {
        if (animationState.isMoving()) {
            if (this.isSprinting()) {
                animationState.getController().setAnimation(RawAnimation.begin().then("run", Animation.LoopType.LOOP));
            } else {
                animationState.getController().setAnimation(RawAnimation.begin().then("walk", Animation.LoopType.LOOP));
            }
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
