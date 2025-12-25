package com.jeremyseq.inhabitants.entities.impaler;

import com.jeremyseq.inhabitants.particles.ImpalerSpikeRaiseParticle;
import com.jeremyseq.inhabitants.particles.ModParticles;
import com.jeremyseq.inhabitants.entities.goals.BreakTorchGoal;
import com.jeremyseq.inhabitants.entities.goals.SprintAtTargetGoal;
import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

import java.util.Random;

public class ImpalerEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public static final int THORN_DAMAGE = 6;
    public static final int SCREAM_COOLDOWN = 300;
    public int screamCooldown = 0;
    public static final EntityDataAccessor<Boolean> SPIKED = SynchedEntityData.defineId(ImpalerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> SCREAM_TRIGGER = SynchedEntityData.defineId(ImpalerEntity.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Integer> TEXTURE = SynchedEntityData.defineId(ImpalerEntity.class, EntityDataSerializers.INT);

    private int spiked_client_timer = -1;

    private int attackAnimTimer = 0;

    public ImpalerEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setMaxUpStep(1.5f);
    }

    public static AttributeSupplier setAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30f)
                .add(Attributes.ATTACK_DAMAGE, 8f)
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
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RestrictSunGoal(this));
        this.goalSelector.addGoal(2, new FleeSunGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new ImpalerRageGoal(this));
        this.goalSelector.addGoal(4, new ImpalerScreamGoal(this));
        this.goalSelector.addGoal(5, new SprintAtTargetGoal(this, 1.4D, 7, 2));
        this.goalSelector.addGoal(6, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(7, new BreakTorchGoal(this, 1));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
    }

    @Override
    public void tick() {
        super.tick();

        screamCooldown = Math.max(0, screamCooldown - 1);

        // regenerate health over time
        if (this.getTarget() == null && this.tickCount % 60 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(1.0F);
        }

        if (this.level().isClientSide) {
            if (spiked_client_timer == 0) {
                spiked_client_timer--;
                ImpalerSpikeRaiseParticle.spawnSpikeBurst((ClientLevel) this.level(), this, 20);
            }
            if (spiked_client_timer > -1) {
                spiked_client_timer--;
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.IMPALER_SPIKE.get(), new Random().nextInt(2, 4)));
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
        if (this.level().isClientSide && pKey == SCREAM_TRIGGER) {
            if (this.entityData.get(SCREAM_TRIGGER)) {
                this.spawnRageParticles();
                Vec3 pos = new Vec3(getX(), getY() + 0.5, getZ());
                Vec3 lookAngle = new Vec3(getLookAngle().x, 0, getLookAngle().z).normalize();
                pos = pos.add(lookAngle.scale(4));
                float yaw = (float) Math.atan2(lookAngle.z, lookAngle.x);
                this.level().addParticle(ModParticles.IMPALER_SCREAM.get(), pos.x, pos.y, pos.z, 0, yaw, 0);
            }
        }
        if (this.level().isClientSide && pKey == SPIKED) {
            if (this.entityData.get(SPIKED)) {
                spiked_client_timer = 8;
            }
        }
    }

    private void spawnRageParticles() {

        RandomSource rand = this.level().random;

        float halfW  = this.getBbWidth() * 0.5f;
        float height = this.getBbHeight();
        Vec3 centre  = new Vec3(
                this.getX(),
                this.getY() + height * 0.5,
                this.getZ()
        );

        for (int i = 0; i < 16; i++) {
            double px = this.getX() + (rand.nextDouble() * 2 - 1) * halfW;
            double py = this.getY() + rand.nextDouble() * height;
            double pz = this.getZ() + (rand.nextDouble() * 2 - 1) * halfW;

            double offX = px - centre.x;
            double offY = py - centre.y;
            double offZ = pz - centre.z;

            double scale = 0.05;
            double dx = offX * scale;
            double dy = offY * scale + 0.02;
            double dz = offZ * scale;

            this.level().addParticle(
                    ModParticles.IMPALER_RAGE.get(),
                    px, py, pz,
                    dx, dy, dz
            );
        }
    }



    @Override
    protected void customServerAiStep() {
        if (this.getTarget() == null) {
            this.getEntityData().set(SPIKED, false);
        }
        if (this.attackAnimTimer > 0) {
            this.attackAnimTimer--;
            if (this.attackAnimTimer == 0) {
                LivingEntity target = getTarget();
                if (target != null && distanceToSqr(target) <= this.getMeleeAttackRangeSqr(target)) {
                    super.doHurtTarget(target);
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
        controllers.add(new AnimationController<>(this, "scream", 0, state -> PlayState.STOP)
                .triggerableAnim("scream", RawAnimation.begin().then("scream", Animation.LoopType.PLAY_ONCE)));
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
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SPIKED, false);
        entityData.define(SCREAM_TRIGGER, true);
        entityData.define(TEXTURE, getBiomeTextureType());
    }

    private int getBiomeTextureType() {
        if (this.level().getBiome(this.blockPosition()).is(Biomes.DRIPSTONE_CAVES)) {
            return 1;
        } else {
            return 0;
        }
    }

    public int getTextureType() {
        return entityData.get(TEXTURE);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor pLevel,
                                                  @NotNull DifficultyInstance pDifficulty,
                                                  @NotNull MobSpawnType pReason,
                                                  @Nullable SpawnGroupData pSpawnData,
                                                  @Nullable CompoundTag pDataTag) {
        if (pSpawnData == null) {
            this.entityData.set(TEXTURE, getBiomeTextureType());
        }
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("textureType", entityData.get(TEXTURE));
        tag.putInt("screamCooldown", this.screamCooldown);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("screamCooldown")) {
            this.screamCooldown = tag.getInt("screamCooldown");
        }

        if (tag.contains("textureType")) {
            entityData.set(TEXTURE, tag.getInt("textureType"));
        } else {
            entityData.set(TEXTURE, getBiomeTextureType());
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
