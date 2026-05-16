package com.jeremyseq.inhabitants.entities.bulltoad;

import com.jeremyseq.inhabitants.debug.DefaultDebugRenderer;
import com.jeremyseq.inhabitants.debug.DevMode;
import com.jeremyseq.inhabitants.entities.ModEntities;
import com.jeremyseq.inhabitants.entities.bulltoad.goals.BulltoadAttackGoal;
import com.jeremyseq.inhabitants.entities.bulltoad.goals.BulltoadBreedGoal;
import com.jeremyseq.inhabitants.entities.bulltoad.goals.BulltoadBullfightGoal;
import com.jeremyseq.inhabitants.entities.bulltoad.goals.BulltoadJumpGoal;
import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.Objects;

public class BulltoadEntity extends Animal implements GeoEntity {

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public static final Ingredient TEMPTATION_ITEM = Ingredient.of(Items.SLIME_BALL);

    public static final EntityDataAccessor<Boolean> JUMPING = SynchedEntityData.defineId(BulltoadEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> TONGUE_OUT = SynchedEntityData.defineId(BulltoadEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Vector3f> TONGUE_LENGTH = SynchedEntityData.defineId(BulltoadEntity.class, EntityDataSerializers.VECTOR3);
    public static final EntityDataAccessor<Boolean> SNAP_YAW = SynchedEntityData.defineId(BulltoadEntity.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Boolean> HAS_HORNS = SynchedEntityData.defineId(BulltoadEntity.class, EntityDataSerializers.BOOLEAN);

    // 0 = not breeded, 1 = stage 1, 2 = stage 2
    public static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(BulltoadEntity.class, EntityDataSerializers.INT);

    private static final String BREED_STAGE_KEY = "BreedStage";
    private static final String BREED_TICKS_KEY = "BreedTicks";
    private static final String HAS_HORNS_KEY = "HasHorns";
    private static final String GROW_HORNS_TICKS_KEY = "GrowHornsTicks";

    private int breedTicks = 0;
    private static final int TICKS_PER_BREED_STAGE = 600; // 30 seconds per stage

    private int growHornsTicks = 0;
    private static final int GROW_HORNS_TICKS = 20 * 60;

    private static final float ADULT_SPEED = .18f;
    private static final float BABY_SPEED = .1f;

    @Nullable
    public BulltoadEntity fightRival = null;
    public boolean fightReadyToLeap = false;
    public int bullfightCooldown = 0;

    public BulltoadEntity(EntityType<? extends Animal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setPathfindingMalus(BlockPathTypes.WATER, -2f);
        this.setPathfindingMalus(BlockPathTypes.LAVA, -2f);
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30f)
                .add(Attributes.ATTACK_DAMAGE, 8f)
                .add(Attributes.ATTACK_SPEED, 1.0f)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5F)
                .add(Attributes.FOLLOW_RANGE, 30f)
                .add(Attributes.MOVEMENT_SPEED, ADULT_SPEED).build();
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BulltoadAttackGoal(this));
        this.goalSelector.addGoal(2, new BulltoadBreedGoal(this, 1.0D));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class,
                10, true, false,
                (entity) -> {
                    if (this.isBaby()) return false;
                    // check if a nearby baby bulltoad was recently hurt by this player
                    return !this.level().getEntitiesOfClass(BulltoadEntity.class,
                            this.getBoundingBox().inflate(16),
                            baby -> baby.isBaby()
                                    && baby.getLastHurtByMob() == entity
                                    && baby.tickCount - baby.getLastHurtByMobTimestamp() < 100
                    ).isEmpty();
                }));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1f, TEMPTATION_ITEM, false));
        this.goalSelector.addGoal(5, new BulltoadBullfightGoal(this));
        this.goalSelector.addGoal(7, new BulltoadJumpGoal(this));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));

        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 30f, 1));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class,
                10, true, false, (entity) -> this.getBreedStage() > 0));
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(@NotNull ServerLevel pLevel, @NotNull AgeableMob pOtherParent) {
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::animationPredicate));
        controllers.add(new AnimationController<>(this, "jump", 0, this::jumpPredicate));
        controllers.add(new AnimationController<>(this, "croaking", 0, state -> PlayState.STOP)
                .triggerableAnim("croaking", RawAnimation.begin().then("croaking", Animation.LoopType.PLAY_ONCE)));
    }

    private <T extends GeoAnimatable> PlayState jumpPredicate(AnimationState<T> state) {
        AnimationController<?> controller = state.getController();
        if (entityData.get(JUMPING)) {
            controller.setAnimation(RawAnimation.begin().then("jumping", Animation.LoopType.HOLD_ON_LAST_FRAME));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    private <T extends GeoAnimatable> PlayState animationPredicate(AnimationState<T> state) {
        AnimationController<?> controller = state.getController();

        if (entityData.get(JUMPING)) {
            controller.setAnimation(RawAnimation.begin().then("jumping", Animation.LoopType.HOLD_ON_LAST_FRAME));
            return PlayState.CONTINUE;
        }

        boolean isMoving = state.isMoving()
                || Math.abs(this.getDeltaMovement().x) > 0.01
                || Math.abs(this.getDeltaMovement().z) > 0.01;

        if (isMoving) {
            controller.setAnimation(RawAnimation.begin().then("walking", Animation.LoopType.LOOP));
        } else {
            controller.setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
        }

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(JUMPING, false);
        entityData.define(STAGE, 0);
        entityData.define(TONGUE_OUT, false);
        entityData.define(TONGUE_LENGTH, new Vector3f(0, 0, 0));
        entityData.define(SNAP_YAW, false);
        entityData.define(HAS_HORNS, true);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {

            // render pathfinding in dev mode
            if (DevMode.showBulltoadPathfinding()) {
                DefaultDebugRenderer.renderPath((ServerLevel) this.level(), this.getNavigation().getPath(), null, this.position());
            }

            // occasional croak when idle
            if (this.random.nextInt(400) == 0) {
                this.triggerAnim("croaking", "croaking");
            }

            // breed ticking
            if (this.getBreedStage() > 0) {
                this.breedTicks++;
            }
            if (this.breedTicks >= TICKS_PER_BREED_STAGE) {
                if (this.getBreedStage() == 2) {
                    for (int i = 0; i < 3; ++i) {
                        BulltoadEntity baby = ModEntities.BULLTOAD.get().create(level());
                        if (baby != null) {
                            baby.setBaby(true);
                            baby.moveTo(this.getX() + (this.random.nextDouble() - 0.5) * 2, this.getY(), this.getZ() + (this.random.nextDouble() - 0.5) * 2, 0, 0);
                            level().addFreshEntity(baby);
                        }
                    }

                    this.setBreedStage(0);
                } else {
                    this.setBreedStage(this.getBreedStage() + 1);
                }
            }

            // horns ticking
            if (!this.hasHorns()) {
                this.growHornsTicks++;
                if (this.growHornsTicks >= GROW_HORNS_TICKS) {
                    this.setHasHorns(true);
                    this.growHornsTicks = 0;
                }
            }

            if (this.bullfightCooldown > 0) {
                this.bullfightCooldown--;
            }
        }
    }

    @Override
    public boolean isFood(@NotNull ItemStack pStack) {
        return TEMPTATION_ITEM.test(pStack);
    }

    @Override
    protected int calculateFallDamage(float pFallDistance, float pDamageMultiplier) {
        return super.calculateFallDamage(pFallDistance, pDamageMultiplier) - 5;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean canFallInLove() {
        return super.canFallInLove() && this.getBreedStage() == 0;
    }

    public void setBreedStage(int stage) {
        this.entityData.set(STAGE, Math.min(Math.max(stage, 0), 2));
        breedTicks = 0;
    }

    public int getBreedStage() {
        return this.entityData.get(STAGE);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt(BREED_STAGE_KEY, entityData.get(STAGE));
        pCompound.putInt(BREED_TICKS_KEY, this.breedTicks);
        pCompound.putBoolean(HAS_HORNS_KEY, this.hasHorns());
        pCompound.putInt(GROW_HORNS_TICKS_KEY, this.growHornsTicks);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        entityData.set(STAGE, pCompound.getInt(BREED_STAGE_KEY));
        this.breedTicks = pCompound.getInt(BREED_TICKS_KEY);
        this.setHasHorns(pCompound.getBoolean(HAS_HORNS_KEY));
        this.growHornsTicks = pCompound.getInt(GROW_HORNS_TICKS_KEY);
    }

    @Override
    public boolean isInWall() {
        AABB box = this.makeBoundingBox().deflate(0.001D);
        return !this.level().noCollision(this, box);
    }

    @Override
    protected @NotNull AABB makeBoundingBox() {
        float width = this.isBaby() ? .5f : 2f;
        float height = this.isBaby() ? .5f : 1.6f;
        double half = width / 2.0;
        return new AABB(getX() - half, getY(), getZ() - half, getX() + half, getY() + height, getZ() + half);
    }

    @Override
    public void setBaby(boolean baby) {
        super.setBaby(baby);
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            Objects.requireNonNull(this.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(baby ? BABY_SPEED : ADULT_SPEED);
        }
    }

    public Vec3 getTongueTarget() {
        Vector3f vec3f = this.entityData.get(TONGUE_LENGTH);
        return new Vec3(vec3f.x, vec3f.y, vec3f.z);
    }

    public boolean isTongueOut() {
        return this.entityData.get(TONGUE_OUT);
    }

    public void setTongueTarget(Vec3 target) {
        this.entityData.set(TONGUE_OUT, true);
        this.entityData.set(TONGUE_LENGTH, new Vector3f((float) target.x, (float) target.y, (float) target.z));
    }

    public void clearTongueTarget() {
        this.entityData.set(TONGUE_OUT, false);
        this.entityData.set(TONGUE_LENGTH, new Vector3f(0, 0, 0));
    }

    public void setSnapYaw(boolean snapYaw) {
        this.entityData.set(SNAP_YAW, snapYaw);
    }

    public boolean shouldSnapYaw() {
        return this.entityData.get(SNAP_YAW);
    }

    public boolean hasHorns() {
        return this.entityData.get(HAS_HORNS);
    }

    private void setHasHorns(boolean horns) {
        this.entityData.set(HAS_HORNS, horns);
    }

    /**
     * removes and drops bulltoad's horns
     */
    public void dropHorns() {
        setHasHorns(false);
        ItemStack horns = new ItemStack(ModItems.BULLTOAD_HORN.get());
        this.spawnAtLocation(horns);
    }

    public boolean isFightReadyToLeap() {
        return fightReadyToLeap;
    }

    public void snapToFaceTargetEntity(LivingEntity target) {
        Vec3 diff = target.position().subtract(this.position());
        float yaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.setYHeadRot(yaw);
        this.yHeadRotO = yaw;
        this.setYBodyRot(yaw);
    }
}
