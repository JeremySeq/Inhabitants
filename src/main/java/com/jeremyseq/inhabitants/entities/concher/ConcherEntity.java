package com.jeremyseq.inhabitants.entities.concher;

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
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

public class ConcherEntity extends WaterAnimal implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(ConcherEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> GROWTH_INHIBITED = SynchedEntityData.defineId(ConcherEntity.class, EntityDataSerializers.BOOLEAN);

    // growth (server side)
    private int growTimer = 0; // in seconds
    private static final int TICKS_PER_GROW_CHECK = 20; // check growth once per second
    private static final int GROW_THRESHOLD_SECONDS = 30; // time to grow

    public ConcherEntity(EntityType<? extends WaterAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
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
    }

    public int getStage() {
        return this.entityData.get(STAGE);
    }

    public void setStage(int stage) {
        int clamped = Math.max(0, Math.min(2, stage));
        this.entityData.set(STAGE, clamped);
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

    }

    @Override
    public void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.5D));
        this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0D, 40));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            // increment growTimer once per second and attempt growth
            if (this.tickCount % TICKS_PER_GROW_CHECK == 0) {
                if (!isGrowthInhibited() && this.getStage() < 2) {
                    growTimer++;
                    if (growTimer >= GROW_THRESHOLD_SECONDS) {
                        int old = this.getStage();
                        this.setStage(old + 1);
                        growTimer = 0;
                        this.onGrowStage(old, this.getStage());
                    }
                }
            }

            // sinking behavior
            if (this.isInWater() && !this.onGround()) {
                double y = this.getDeltaMovement().y - 0.02D;
                if (y < -0.5D) y = -0.5D;
                this.setDeltaMovement(this.getDeltaMovement().x, y, this.getDeltaMovement().z);
            }
        }
    }

    protected void onGrowStage(int oldStage, int newStage) {
        // prevent entity from being stuck inside blocks when
        // if new bounding box collides, find a nearby safe position, if none, revert stage

        if (this.level().isClientSide()) return;
        ServerLevel serverLevel = (ServerLevel) this.level();

        AABB newBox = this.makeBoundingBox();
        // no issue
        if (serverLevel.noCollision(this, newBox)) {
            spawnGrowthParticles(serverLevel);
            return;
        }

        // try to find a nearby safe location using small offsets
        double baseX = this.getX();
        double baseY = this.getY();
        double baseZ = this.getZ();
        boolean found = false;
        double[] vertSteps = {0.25D, 0.5D, 0.75D, 1.0D, 1.25D, 1.5D, 1.75D, 2.0D};
        double[] horizSteps = {0.0D, 0.5D, -0.5D, 1.0D, -1.0D};

        for (double vy : vertSteps) {
            for (double dx : horizSteps) {
                for (double dz : horizSteps) {
                    AABB moved = newBox.move(dx, vy, dz);
                    if (serverLevel.noCollision(this, moved)) {
                        // move entity to safe location and finalize growth
                        this.setPos(baseX + dx, baseY + vy, baseZ + dz);
                        this.refreshDimensions();
                        spawnGrowthParticles(serverLevel);
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
            if (found) break;
        }

        if (!found) {
            // could not find room for the larger size -> revert stage and postpone growth
            this.setStage(oldStage);
            this.refreshDimensions();
            // shorten the retry so it will try again after some time
            this.growTimer = Math.max(0, GROW_THRESHOLD_SECONDS / 2);
            // spawn inhibit particles to show growth failed
            spawnInhibitParticles(serverLevel);
        }
    }

    private void spawnInhibitParticles(ServerLevel serverLevel) {
        for (int i = 0; i < 8; i++) {
            double px = this.getX() + (this.random.nextDouble() - 0.5D) * (double)this.getCurrentSize()[0];
            double py = this.getY() + this.random.nextDouble() * (double)this.getCurrentSize()[1];
            double pz = this.getZ() + (this.random.nextDouble() - 0.5D) * (double)this.getCurrentSize()[0];
            serverLevel.sendParticles(ParticleTypes.CLOUD, px, py, pz, 1, 0.0D, 0.05D, 0.0D, 0.0D);
        }
    }

    private void spawnGrowthParticles(ServerLevel serverLevel) {
        for (int i = 0; i < 8; i++) {
            double px = this.getX() + (this.random.nextDouble() - 0.5D) * (double)this.getCurrentSize()[0];
            double py = this.getY() + this.random.nextDouble() * (double)this.getCurrentSize()[1];
            double pz = this.getZ() + (this.random.nextDouble() - 0.5D) * (double)this.getCurrentSize()[0];
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
                    if (!player.getAbilities().instabuild) stack.shrink(1);
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
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ConcherStage")) this.setStage(tag.getInt("ConcherStage"));
        if (tag.contains("ConcherGrowthInhibited")) this.entityData.set(GROWTH_INHIBITED, tag.getBoolean("ConcherGrowthInhibited"));
        if (tag.contains("ConcherGrowTimer")) this.growTimer = tag.getInt("ConcherGrowTimer");
    }

    @Override
    protected @NotNull AABB makeBoundingBox() {
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
        float[][] sizes = {{1f, .5f}, {1.5f, 1.25f}, {2f, 2.1f}};
        return sizes[this.getStage()];
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
