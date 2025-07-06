package com.jeremyseq.inhabitants.entities.impaler;

import com.jeremyseq.inhabitants.ModParticles;
import com.jeremyseq.inhabitants.effects.ModEffects;
import com.jeremyseq.inhabitants.entities.EntityUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

public class ImpalerEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public static final int THORN_DAMAGE = 6;
    public static final EntityDataAccessor<Boolean> SPIKED = SynchedEntityData.defineId(ImpalerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> RAGE_TRIGGER = SynchedEntityData.defineId(ImpalerEntity.class, EntityDataSerializers.BOOLEAN);

    private int attackAnimTimer = 0;
    private int rageAnimTimer = 0;

    public ImpalerEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier setAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40f)
                .add(Attributes.ATTACK_DAMAGE, 15f)
                .add(Attributes.ATTACK_SPEED, 1.0f)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5F)
                .add(Attributes.FOLLOW_RANGE, 30f)
                .add(Attributes.MOVEMENT_SPEED, .25f).build();
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 30f, 1));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.addBehaviourGoals();
    }

    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(1, new RestrictSunGoal(this));
        this.goalSelector.addGoal(2, new FleeSunGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new SprintAtTargetGoal(this, 1.4D, 7, 2));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new BreakTorchGoal(this, 1));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
    }

    @Override
    public void tick() {
        super.tick();

        // regenerate health over time
        if (this.getTarget() == null && this.tickCount % 60 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(1.0F);
        }
        if (this.isSpiked() && this.getHealth() > this.getAttributeValue(Attributes.MAX_HEALTH)/2) {
            this.entityData.set(SPIKED, false);
        }
    }

    @Override
    public void setSprinting(boolean pSprinting) {

        if (this.isSprinting() && !pSprinting) {
            // stop sprinting
            this.triggerAnim("sprint", "stopSprint");
        } else if (!this.isSprinting() && pSprinting) {
            // start sprinting
            this.triggerAnim("sprint", "startSprint");
        }

        super.setSprinting(pSprinting);
    }

    public void aiStep() {
        // burn in sunlight
        if (this.isAlive()) {
            boolean flag = this.isSunBurnTick();
            if (flag) {
                ItemStack itemstack = this.getItemBySlot(EquipmentSlot.HEAD);
                if (!itemstack.isEmpty()) {
                    if (itemstack.isDamageableItem()) {
                        itemstack.setDamageValue(itemstack.getDamageValue() + this.random.nextInt(2));
                        if (itemstack.getDamageValue() >= itemstack.getMaxDamage()) {
                            this.broadcastBreakEvent(EquipmentSlot.HEAD);
                            this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                        }
                    }

                    flag = false;
                }

                if (flag) {
                    this.setSecondsOnFire(8);
                }
            }
        }

        super.aiStep();
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if (this.level().isClientSide && pKey == RAGE_TRIGGER) {
            if (this.entityData.get(RAGE_TRIGGER)) {
                Vec3 pos = new Vec3(getX(), getY() + 0.5, getZ());
                Vec3 lookAngle = new Vec3(getLookAngle().x, 0, getLookAngle().z).normalize();
                pos = pos.add(lookAngle.scale(4));
                float yaw = (float) Math.atan2(lookAngle.z, lookAngle.x);
                this.level().addParticle(ModParticles.IMPALER_SCREAM.get(), pos.x, pos.y, pos.z, 0, yaw, 0);
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        if (this.attackAnimTimer > 0) {
            this.attackAnimTimer--;
            if (this.attackAnimTimer == 0) {
                LivingEntity target = getTarget();
                if (target != null && distanceToSqr(target) <= this.getMeleeAttackRangeSqr(target)) {
                    super.doHurtTarget(target);
                }
            }
        }
        entityData.set(RAGE_TRIGGER, this.rageAnimTimer == 0);

        if (this.rageAnimTimer > 0) {
            this.rageAnimTimer--;
            if (this.rageAnimTimer == 0) {
                EntityUtil.shockwave(this, 10, 10);
                this.level().playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 10f, 0.9F);
                // give nearby players the concussion effect
                double radius = 15.0D;
                List<Player> players = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(radius));
                for (Player player : players) {
                    player.addEffect(new MobEffectInstance(ModEffects.CONCUSSION.get(), 300, 0));
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        if (!level().isClientSide) {
            triggerAnim("attack", "bite");
            this.attackAnimTimer = 10;
        }
        return true;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (!this.isSpiked() && this.getHealth() <= this.getAttributeValue(Attributes.MAX_HEALTH)/2) {
            this.entityData.set(SPIKED, true);
            this.triggerAnim("rage", "rage");
            this.rageAnimTimer = 10;
            return result;
        }
        if (result && !level().isClientSide) {
            this.triggerAnim("hurt", "hurt");
        }
        if (this.isSpiked() && !source.is(DamageTypes.THORNS)) {
            if (source.getDirectEntity() instanceof LivingEntity livingEntity) {
                livingEntity.hurt(this.damageSources().thorns(this), THORN_DAMAGE);
            }
        }
        return result;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
        controllers.add(new AnimationController<>(this, "hurt", 0, state -> PlayState.STOP)
                .triggerableAnim("hurt", RawAnimation.begin().then("hurt", Animation.LoopType.PLAY_ONCE)));
        controllers.add(new AnimationController<>(this, "attack", 0, state -> PlayState.STOP)
                .triggerableAnim("bite", RawAnimation.begin().then("bite", Animation.LoopType.PLAY_ONCE)));
        controllers.add(new AnimationController<>(this, "rage", 0, state -> PlayState.STOP)
                .triggerableAnim("rage", RawAnimation.begin().then("rage", Animation.LoopType.PLAY_ONCE)));
        controllers.add(new AnimationController<>(this, "sprint", 0, state -> PlayState.STOP)
                .triggerableAnim("startSprint", RawAnimation.begin().then("stepping on four", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("stopSprint", RawAnimation.begin().then("stepping on two", Animation.LoopType.PLAY_ONCE)));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> animationState) {
        if (animationState.isMoving()) {
            if (this.isSprinting()) {
                animationState.getController().setAnimation(RawAnimation.begin().then("sprint", Animation.LoopType.LOOP));
            } else {
                animationState.getController().setAnimation(RawAnimation.begin().then("walk", Animation.LoopType.LOOP));
            }
        } else {
            animationState.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
        }

        return PlayState.CONTINUE;
    }

    /**
     * @return whether the impaler has its spikes out.
     */
    public boolean isSpiked() {
        return this.entityData.get(SPIKED);
    }

    @Override
    public float getStepHeight() {
        return 1.5f;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SPIKED, false);
        entityData.define(RAGE_TRIGGER, false);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
