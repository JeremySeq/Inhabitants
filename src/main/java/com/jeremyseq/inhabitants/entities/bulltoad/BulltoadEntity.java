package com.jeremyseq.inhabitants.entities.bulltoad;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.entities.bulltoad.goals.BulltoadBreedGoal;
import com.jeremyseq.inhabitants.entities.bulltoad.goals.BulltoadJumpGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;

public class BulltoadEntity extends Animal implements GeoEntity {

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public static final Ingredient TEMPTATION_ITEM = Ingredient.of(Items.SLIME_BALL);

    public static final EntityDataAccessor<Boolean> CROAKING = SynchedEntityData.defineId(BulltoadEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> JUMPING = SynchedEntityData.defineId(BulltoadEntity.class, EntityDataSerializers.BOOLEAN);

    // 0 = not breeded, 1 = stage 1, 2 = stage 2
    public static final EntityDataAccessor<Integer> BREED_STAGE = SynchedEntityData.defineId(BulltoadEntity.class, EntityDataSerializers.INT);

    private static final String BREED_STAGE_KEY = "BreedStage";
    private static final String BREED_TICKS_KEY = "BreedTicks";

    private int breed_ticks = 0;
    private static final int TICKS_PER_BREED_STAGE = 600; // 30 seconds per stage

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
                .add(Attributes.MOVEMENT_SPEED, 0.2f).build();
    }

    protected void registerGoals() {this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new BulltoadBreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1f, TEMPTATION_ITEM, false));
        this.goalSelector.addGoal(7, new BulltoadJumpGoal(this));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));

        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 30f, 1));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(@NotNull ServerLevel pLevel, @NotNull AgeableMob pOtherParent) {
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::animationPredicate));
        controllers.add(new AnimationController<>(this, "jump", 0, this::jumpPredicate));
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

        if (entityData.get(CROAKING)) {
            controller.setAnimation(RawAnimation.begin().then("croaking", Animation.LoopType.PLAY_ONCE));
            if (controller.hasAnimationFinished()) {
                entityData.set(CROAKING, false);
                controller.forceAnimationReset();
            }
            return PlayState.CONTINUE;
        }

        if (state.isMoving()) {
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
        entityData.define(CROAKING, false);
        entityData.define(JUMPING, false);
        entityData.define(BREED_STAGE, 0);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            // occasional croak when idle
            if (this.onGround() && !this.getNavigation().isDone() && this.random.nextInt(400) == 0) {
                this.entityData.set(CROAKING, true);
            } else if (this.onGround() && this.random.nextInt(1000) == 0) {
                this.entityData.set(CROAKING, true);
            }

            // breed ticking
            if (this.getBreedStage() != 0) {
                this.breed_ticks++;
            }
            if (this.breed_ticks >= TICKS_PER_BREED_STAGE) {
                if (this.getBreedStage() == 2) {
                    // TODO: spawn babies
                    Inhabitants.LOGGER.debug("spawn babies");
                    this.setBreedStage(0);
                } else {
                    this.setBreedStage(this.getBreedStage() + 1);
                }
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
        this.entityData.set(BREED_STAGE, Math.min(Math.max(stage, 0), 2));
        breed_ticks = 0;
    }

    public int getBreedStage() {
        return this.entityData.get(BREED_STAGE);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt(BREED_STAGE_KEY, entityData.get(BREED_STAGE));
        pCompound.putInt(BREED_TICKS_KEY, this.breed_ticks);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        entityData.set(BREED_STAGE, pCompound.getInt(BREED_STAGE_KEY));
        this.breed_ticks = pCompound.getInt(BREED_TICKS_KEY);
    }
}
