package com.jeremyseq.inhabitants.entities.gazer;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.entities.gazer_pod.GazerPodEntity;
import com.jeremyseq.inhabitants.items.GazerPodItem;
import com.jeremyseq.inhabitants.items.ModItems;
import com.jeremyseq.inhabitants.networking.GazerCameraPacketS2C;
import com.jeremyseq.inhabitants.networking.ModNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class GazerEntity extends FlyingMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Random random = new Random();

    private int podEntryTick = -1;

    private GazerPodEntity returningPod = null;
    private Path returningPath = null;

    private static final EntityDataAccessor<String> STATE =
            SynchedEntityData.defineId(GazerEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(GazerEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final EntityDataAccessor<Boolean> ENTERING_POD =
            SynchedEntityData.defineId(GazerEntity.class, EntityDataSerializers.BOOLEAN);


    public GazerEntity(EntityType<? extends FlyingMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 600f)
                .add(Attributes.FLYING_SPEED, 2f)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .build();
    }

    public enum GazerState {
        IDLE,               // Floating near the pod
        BEING_CONTROLLED,   // Player is “possessing” the gazer
        RETURNING_TO_POD    // Returning to pod after being controlled
    }

    @Override
    public float getEyeHeight(Pose pPose) {
        return super.getEyeHeight(pPose) + 0.5f;
    }

    @Override
    public double getEyeY() {
        return super.getEyeY();
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        // no fall damage
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return false;
    }

    @Override
    protected void registerGoals() {
        // move to player holding pod item when entering pod
        this.goalSelector.addGoal(1, new FollowPodHolderGoal(this));

        // Random floating movement when IDLE
        this.goalSelector.addGoal(2, new GazerWanderGoal(this));

        // Look around when IDLE
        this.goalSelector.addGoal(2, new GazerLookAroundGoal(this));
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level pLevel) {
        FlyingPathNavigation flyingpathnavigation = new FlyingPathNavigation(this, pLevel) {
//            @Override
//            public boolean isStableDestination(BlockPos blockPos) {
//                return !this.level.getBlockState(blockPos.below()).isAir();
//            }
        };
        flyingpathnavigation.setCanOpenDoors(false);
        flyingpathnavigation.setCanFloat(false);
        flyingpathnavigation.setCanPassDoors(true);
        return flyingpathnavigation;
    }

    @Override
    public void tick() {
        super.tick();

        // if being controlled, check owner validity
        if (this.getGazerState() == GazerState.BEING_CONTROLLED && !level().isClientSide) {
            ServerPlayer owner = (ServerPlayer) this.level().getPlayerByUUID(this.getOwnerUUID());
            if (owner == null
                    || owner.isDeadOrDying()
                    || owner.getItemBySlot(EquipmentSlot.HEAD).getItem() != ModItems.GAZER_POD.get()) {

                // force stop
                this.setGazerState(GazerState.IDLE);
                this.setOwnerUUID(null);

                if (owner != null && !owner.level().isClientSide) {
                    ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> owner),
                            new GazerCameraPacketS2C(this.getId(), false));
                }
            }
        }

        // trigger RETURNING TO POD state
        if (this.getGazerState() == GazerState.IDLE && random.nextInt(50) == 0) {
            // find closest GazerPodEntity without a gazer
            this.setGazerState(GazerState.RETURNING_TO_POD);
        }

        // handle RETURNING TO POD state
        if (this.getGazerState() == GazerState.RETURNING_TO_POD) {
            if (returningPod == null || !returningPod.isAlive() || returningPod.hasGazer()) {
                // Find closest pod
                double closestDistance = Double.MAX_VALUE;
                for (Entity entity : this.level().getEntities(this, this.getBoundingBox().inflate(50))) {
                    if (entity instanceof GazerPodEntity pod && !pod.hasGazer()) {
                        double distance = this.distanceToSqr(pod);
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            returningPod = pod;
                        }
                    }
                }
                if (returningPod != null) {
                    returningPath = this.getNavigation().createPath(returningPod, 0);
                    if (returningPath != null) {
                        this.getNavigation().moveTo(returningPath, 1.0);
                        Inhabitants.LOGGER.debug("GazerEntity {} returning to pod {}", this.getUUID(), returningPod.getUUID());
                    }
                } else {
                    this.setGazerState(GazerState.IDLE);
                }
            } else {
                // Only recalculate if navigation is done
                if (this.getNavigation().isDone() && this.distanceTo(returningPod) >= 2.0) {
                    returningPath = this.getNavigation().createPath(returningPod, 0);
                    if (returningPath != null) {
                        this.getNavigation().moveTo(returningPath, 1.0);
                    }
                }
                // If close enough, enter pod
                if (this.distanceTo(returningPod) < 2.0) {
                    this.enterPod();
                    returningPod.setHasGazer(true);
                    returningPod = null;
                    returningPath = null;
                }
            }
        }

        // handle pod entry discard timing
        if (!level().isClientSide && isEnteringPod() && podEntryTick > 0 && this.tickCount - podEntryTick > 40) {
            this.discard();
        }
    }

    @Override
    public boolean isAlwaysTicking() {
        return true;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);

        if (this.getOwnerUUID() == null) return;
        Player player = this.level().getPlayerByUUID(this.getOwnerUUID());
        if (player == null) return;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof GazerPodItem) {
                if (GazerPodItem.getGazerId(stack) == this.getUUID()) {
                    GazerPodItem.removeGazerId(stack);
                }
            }
        }
    }

    // ----- State Transitions -----

    public void enterPod() {

        this.setEnteringPod(true);

        if (!level().isClientSide) {
            podEntryTick = this.tickCount;
        }
    }

    public void exitPod(boolean controlled) {
        this.setGazerState(controlled ? GazerState.BEING_CONTROLLED : GazerState.IDLE);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER, Optional.empty());
        this.entityData.define(ENTERING_POD, false);
        this.entityData.define(STATE, GazerState.IDLE.name());
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        // Save custom data
        if (this.getOwnerUUID() != null) {
            tag.putUUID("Owner", this.getOwnerUUID());
        }
        tag.putString("GazerState", this.getGazerState().name());
        tag.putBoolean("EnteringPod", this.isEnteringPod());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        // Load custom data
        if (tag.hasUUID("Owner")) {
            this.setOwnerUUID(tag.getUUID("Owner"));
        } else {
            this.setOwnerUUID(null);
        }
        if (tag.contains("GazerState")) {
            this.setGazerState(GazerState.valueOf(tag.getString("GazerState")));
        }
        this.setEnteringPod(tag.getBoolean("EnteringPod"));
    }


    // ----- Synced Data Getters/Setters -----
    public GazerState getGazerState() {
        return GazerState.valueOf(this.entityData.get(STATE));
    }
    public void setGazerState(GazerState state) {
        this.entityData.set(STATE, state.name());
    }
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER).orElse(null);
    }
    public void setOwnerUUID(UUID uuid) {
        this.entityData.set(OWNER, Optional.ofNullable(uuid));
    }
    public boolean isEnteringPod() {
        return this.entityData.get(ENTERING_POD);
    }
    public void setEnteringPod(boolean entering) {
        this.entityData.set(ENTERING_POD, entering);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> animationState) {
        AnimationController<?> controller = animationState.getController();

        if (this.isEnteringPod()) {
            controller.setAnimation(RawAnimation.begin().then("landing into pod", Animation.LoopType.PLAY_ONCE));
            if (controller.hasAnimationFinished()) {
                Inhabitants.LOGGER.debug("GazerEntity entering pod animation finished, removing entity");
                this.discard(); // remove entity after animation for client side
            }
            return PlayState.CONTINUE;
        }

        if (this.getDeltaMovement().lengthSqr() > 0.03) {
            controller.setAnimation(RawAnimation.begin().then("floating_movement", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        } else {
            controller.setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
        }

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
