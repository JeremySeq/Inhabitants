package com.jeremyseq.inhabitants.entities.concher;

import com.jeremyseq.inhabitants.entities.concher.ai.ConcherAi;
import com.jeremyseq.inhabitants.entities.concher.render.ConcherAnimationHandler;
import com.jeremyseq.inhabitants.entities.concher.ai.ConcherPathfinding;
import com.jeremyseq.inhabitants.blocks.ConcherShellBlock;
import com.jeremyseq.inhabitants.blocks.ModBlocks;
import com.jeremyseq.inhabitants.entities.ModEntities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;

import org.jetbrains.annotations.NotNull;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

public class ConcherEntity extends WaterAnimal implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(ConcherEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> GROWTH_INHIBITED = SynchedEntityData.defineId(ConcherEntity.class,
            EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> AI_STATE = SynchedEntityData.defineId(ConcherEntity.class,
            EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> SLEEPING_STATE = SynchedEntityData.defineId(ConcherEntity.class,
            EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> BLINKING = SynchedEntityData.defineId(ConcherEntity.class,
            EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> WALK_PAUSING = SynchedEntityData.defineId(ConcherEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHEDDING_ACTIVE = SynchedEntityData.defineId(ConcherEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SHEDDING_DIRECTION_Y = SynchedEntityData
            .defineId(ConcherEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> PANIC_PHASE = SynchedEntityData.defineId(ConcherEntity.class,
            EntityDataSerializers.INT);

    private ConcherAi ai;

    // growth (server side)
    public int growTimer = 0; // in seconds
    private static final int TICKS_PER_GROW_CHECK = 20; // check growth once per second
    private static final int[] GROW_THRESHOLDS = { 300, 600, 900 }; // 5m, 10m, 15m

    private Vec3 sheddingTargetPos = null;
    private float sheddingRotation = 0f;

    public ConcherEntity(EntityType<? extends WaterAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.moveControl = new ConcherPathfinding.ConcherMoveControl(this);
        this.setMaxUpStep(1.0F);
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        return new ConcherPathfinding.ConcherPathNavigation(this, level);
    }

    public static AttributeSupplier setAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0F)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5F)
                .add(Attributes.MOVEMENT_SPEED, .17f).build();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STAGE, 0);
        this.entityData.define(GROWTH_INHIBITED, false);
        this.entityData.define(AI_STATE, 0);
        this.entityData.define(SLEEPING_STATE, 0);
        this.entityData.define(BLINKING, false);
        this.entityData.define(WALK_PAUSING, true);
        this.entityData.define(SHEDDING_ACTIVE, false);
        this.entityData.define(SHEDDING_DIRECTION_Y, 0f);
        this.entityData.define(PANIC_PHASE, 0);
    }

    public int getStage() {
        return this.entityData.get(STAGE);
    }

    public void setStage(int stage) {
        int old = this.getStage();
        int clamped = Math.max(0, Math.min(3, stage));
        this.entityData.set(STAGE, clamped);

        if (old != clamped) {
            this.refreshDimensions();
        }
    }

    public boolean isGrowthInhibited() {
        return this.entityData.get(GROWTH_INHIBITED);
    }

    public void inhibitGrowth() {
        this.entityData.set(GROWTH_INHIBITED, true);
        if (this.level() instanceof ServerLevel serverLevel) {
            spawnInhibitParticles(serverLevel);
        }
    }

    public void resumeGrowth() {
        this.entityData.set(GROWTH_INHIBITED, false);
        if (this.level() instanceof ServerLevel serverLevel) {
            spawnInhibitParticles(serverLevel);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        ConcherAnimationHandler.registerControllers(this, controllerRegistrar);
    }

    @Override
    public void registerGoals() {
        super.registerGoals();
        this.getAI().registerGoals();
        // this.goalSelector.addGoal(2, new PanicGoal(this, 1.5D));
        // this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0D, 40));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // increment growTimer once per second and attempt growth
            if (this.tickCount % TICKS_PER_GROW_CHECK == 0) {
                if (!isGrowthInhibited() && this.getStage() < 3) {
                    growTimer++;
                    if (growTimer >= GROW_THRESHOLDS[this.getStage()]) {
                        int old = this.getStage();
                        this.setStage(old + 1);
                        growTimer = 0;
                        this.getNavigation().stop();
                        this.getAI().onGrowStage(old, this.getStage());
                    }
                }
            }

            // sinking behavior for stages 1-3
            if (this.isInWater() && !this.onGround() && this.getStage() > 0) {
                double y = this.getDeltaMovement().y - 0.02D;
                if (y < -0.5D)
                    y = -0.5D;
                this.setDeltaMovement(this.getDeltaMovement().x, y, this.getDeltaMovement().z);
            }

            // automatic shell shedding when player hold kelp
            if (this.getStage() > 0) {
                Player player = this.level().getNearestPlayer(this, 5.0d);

                if (player != null && player.isHolding(Items.KELP)) {
                    triggerShedding(player);
                }
            }
        }

        if (!this.level().isClientSide() && isSheddingActive()) {
            handleSheddingMovement();
        }
    }

    private void handleSheddingMovement() {
        this.setYRot(this.sheddingRotation);
        this.setYHeadRot(this.sheddingRotation);
        this.setYBodyRot(this.sheddingRotation);
        this.yRotO = this.sheddingRotation;
        this.yBodyRotO = this.sheddingRotation;
        this.yHeadRotO = this.sheddingRotation;

        if (this.sheddingTargetPos != null) {
            Vec3 currentPos = this.position();
            Vec3 direction = this.sheddingTargetPos.subtract(currentPos).normalize();
            Vec3 moveVec = direction.scale(0.08D);

            this.setPos(
                    currentPos.x + moveVec.x,
                    currentPos.y + moveVec.y,
                    currentPos.z + moveVec.z);

            double distToTargetSqr = currentPos.distanceToSqr(this.sheddingTargetPos);
            if (distToTargetSqr < 0.01D ||
                    currentPos.subtract(this.sheddingTargetPos).dot(moveVec) >= 0) {

                setSheddingActive(false);
            }
        } else {
            setSheddingActive(false);
        }
    }

    public boolean isSheddingActive() {
        return this.entityData.get(SHEDDING_ACTIVE);
    }

    public void setSheddingActive(boolean active) {
        this.entityData.set(SHEDDING_ACTIVE, active);
    }

    private void triggerShedding(Player player) {
        if (this.level().isClientSide())
            return;

        BlockState shellState = ModBlocks.CONCHER_SHELL_BLOCK_STAGE_1
                .get().defaultBlockState();

        if (this.getStage() == 2)
            shellState = ModBlocks.CONCHER_SHELL_BLOCK_STAGE_2
                    .get().defaultBlockState();

        if (this.getStage() == 3)
            shellState = ModBlocks.CONCHER_SHELL_BLOCK_STAGE_3
                    .get().defaultBlockState();

        Direction facing = this.getDirection();
        shellState = shellState.setValue(ConcherShellBlock.FACING, facing);
        this.level().setBlock(this.blockPosition(), shellState, 3);

        ConcherEntity newConcher = ModEntities.CONCHER.get().create(this.level());
        if (newConcher != null) {

            float snapRotation = facing.toYRot();

            newConcher.moveTo(
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    snapRotation,
                    this.getXRot());

            newConcher.setStage(0);

            float distance = 1.5f;
            if (this.getStage() == 2)
                distance = 2.5f;
            if (this.getStage() == 3)
                distance = 2.75f;

            Vec3 directionVec = Vec3.directionFromRotation(0, snapRotation);
            newConcher.sheddingTargetPos = this.position().add(directionVec.scale(distance));
            newConcher.sheddingRotation = snapRotation;
            newConcher.setSheddingActive(true);

            this.level().addFreshEntity(newConcher);
        }

        this.discard();
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (isSheddingActive()) {
            return false;
        }

        boolean result = super.hurt(source, amount);

        if (result && !this.level().isClientSide()) {
            this.getAI().onHurt(source);
        }

        return result;
    }

    @Override
    protected void handleAirSupply(int pAirSupply) {
        // prevent Concher from drying outside the water
    }

    @Override
    public void travel(@NotNull Vec3 travelVector) {
        if (this.isControlledByLocalInstance() && this.isInWater() && this.getStage() == 0) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.getAI().aiStep();
    }

    public void spawnInhibitParticles(ServerLevel serverLevel) {
        for (int i = 0; i < 8; i++) {
            double px = this.getX() + (this.random.nextDouble() - 0.5D) * (double) this.getCurrentSize()[0];
            double py = this.getY() + this.random.nextDouble() * (double) this.getCurrentSize()[1];
            double pz = this.getZ() + (this.random.nextDouble() - 0.5D) * (double) this.getCurrentSize()[0];
            serverLevel.sendParticles(ParticleTypes.CLOUD, px, py, pz, 1, 0.0D, 0.05D, 0.0D, 0.0D);
        }
    }

    public void spawnGrowthParticles(ServerLevel serverLevel) {
        for (int i = 0; i < 8; i++) {
            double px = this.getX() + (this.random.nextDouble() - 0.5D) * (double) this.getCurrentSize()[0];
            double py = this.getY() + this.random.nextDouble() * (double) this.getCurrentSize()[1];
            double pz = this.getZ() + (this.random.nextDouble() - 0.5D) * (double) this.getCurrentSize()[0];
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, py, pz, 1, 0.0D, 0.05D, 0.0D, 0.0D);
        }
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // right click with honeycomb to stop growth
        if (stack.getItem() == Items.HONEYCOMB) {
            if (!this.level().isClientSide()) {
                // only consume honeycomb if inhibited state was changed
                if (!this.isGrowthInhibited()) {
                    if (!player.getAbilities().instabuild)
                        stack.shrink(1);
                }
                this.inhibitGrowth();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        // right click with axe to resume growth
        if (stack.getItem() instanceof AxeItem) {
            if (!this.level().isClientSide()) {
                this.resumeGrowth();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ConcherStage", this.getStage());
        tag.putBoolean("ConcherGrowthInhibited", this.isGrowthInhibited());
        tag.putInt("ConcherGrowTimer", this.growTimer);
        tag.putInt("ConcherAIState", this.entityData.get(AI_STATE));
        tag.putInt("ConcherSleepingState", this.entityData.get(SLEEPING_STATE));
        tag.putInt("PanicPhase", this.entityData.get(PANIC_PHASE));

        tag.putBoolean("SheddingActive", isSheddingActive());
        tag.putFloat("SheddingRotation", this.sheddingRotation);
        if (this.sheddingTargetPos != null) {
            tag.putDouble("ShedTargetX", this.sheddingTargetPos.x);
            tag.putDouble("ShedTargetY", this.sheddingTargetPos.y);
            tag.putDouble("ShedTargetZ", this.sheddingTargetPos.z);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ConcherStage"))
            this.setStage(tag.getInt("ConcherStage"));
        if (tag.contains("ConcherGrowthInhibited"))
            this.entityData.set(GROWTH_INHIBITED, tag.getBoolean("ConcherGrowthInhibited"));
        if (tag.contains("ConcherGrowTimer"))
            this.growTimer = tag.getInt("ConcherGrowTimer");
        if (tag.contains("ConcherAIState"))
            this.entityData.set(AI_STATE, tag.getInt("ConcherAIState"));
        if (tag.contains("ConcherSleepingState"))
            this.entityData.set(SLEEPING_STATE, tag.getInt("ConcherSleepingState"));
        if (tag.contains("PanicPhase"))
            this.entityData.set(PANIC_PHASE, tag.getInt("PanicPhase"));

        if (tag.contains("SheddingActive"))
            setSheddingActive(tag.getBoolean("SheddingActive"));
        if (tag.contains("SheddingRotation"))
            this.sheddingRotation = tag.getFloat("SheddingRotation");

        if (tag.contains("ShedTargetX")) {
            this.sheddingTargetPos = new Vec3(
                    tag.getDouble("ShedTargetX"),
                    tag.getDouble("ShedTargetY"),
                    tag.getDouble("ShedTargetZ"));
        }
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        float[] size = this.getCurrentSize();
        return EntityDimensions.scalable(size[0], size[1]);
    }

    @Override
    public @NotNull AABB makeBoundingBox() {
        float width = this.getCurrentSize()[0];
        float height = this.getCurrentSize()[1];
        double half = width / 2.0;
        return new AABB(getX() - half, getY(), getZ() - half, getX() + half, getY() + height, getZ() + half);
    }

    @Override
    public boolean isInWall() {
        AABB box = this.makeBoundingBox().deflate(0.001D);
        return !this.level().noCollision(this, box);
    }

    /**
     * @return [width, height]
     */
    public float[] getCurrentSize() {
        float[][] sizes = { { .85f, .3f }, { 1f, .5f }, { 1.5f, 1.25f }, { 2f, 2.1f } };
        return sizes[this.getStage()];
    }

    public void setBlinking(boolean blinking) {
        this.entityData.set(BLINKING, blinking);
    }

    public boolean isBlinking() {
        return this.entityData.get(BLINKING);
    }

    public boolean isWalkPausing() {
        return this.entityData.get(WALK_PAUSING);
    }

    public void setWalkPausing(boolean pausing) {
        this.entityData.set(WALK_PAUSING, pausing);
    }

    public void setPanicPhase(int phase) {
        this.entityData.set(PANIC_PHASE, phase);
    }

    public int getPanicPhase() {
        return this.entityData.get(PANIC_PHASE);
    }

    public ConcherAi getAI() {
        if (this.ai == null) {
            this.ai = new ConcherAi(this);
        }
        return this.ai;
    }

    public ConcherAi.State getAIState() {
        return ConcherAi.State.values()[this.entityData.get(AI_STATE)];
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
