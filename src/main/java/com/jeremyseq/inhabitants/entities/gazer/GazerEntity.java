package com.jeremyseq.inhabitants.entities.gazer;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.blocks.GazerPodBlockRegistry;
import com.jeremyseq.inhabitants.blocks.entity.GazerPodBlockEntity;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

public class GazerEntity extends FlyingMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int podEntryTick = -1;

    public GazerPodBlockEntity returningPod = null;
    public ItemStack returningPodItem = ItemStack.EMPTY;

    private static final EntityDataAccessor<String> STATE =
            SynchedEntityData.defineId(GazerEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(GazerEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final EntityDataAccessor<Boolean> ENTERING_POD =
            SynchedEntityData.defineId(GazerEntity.class, EntityDataSerializers.BOOLEAN);


    public GazerEntity(EntityType<? extends FlyingMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.LAVA, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, -1.0F);
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10f)
                .add(Attributes.FLYING_SPEED, 2f)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .build();
    }

    public enum GazerState {
        IDLE,               // Floating near the pod
        BEING_CONTROLLED,   // Player is “possessing” the gazer
        RETURNING_TO_POD    // Returning to pod block to enter
    }

    @Override
    public float getEyeHeight(@NotNull Pose pPose) {
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
    protected void checkFallDamage(double y, boolean onGround, @NotNull BlockState state, @NotNull BlockPos pos) {
        // no fall damage
    }

    @Override
    public boolean canAttack(@NotNull LivingEntity target) {
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
        FlyingPathNavigation flyingpathnavigation = new FlyingPathNavigation(this, pLevel);
        flyingpathnavigation.setCanOpenDoors(false);
        flyingpathnavigation.setCanFloat(false);
        flyingpathnavigation.setCanPassDoors(true);
        return flyingpathnavigation;
    }

    @Override
    public void tick() {
        super.tick();

        Player owner = this.level().getPlayerByUUID(this.getOwnerUUID());

        // if being controlled, check owner validity
        if (this.getGazerState() == GazerState.BEING_CONTROLLED && !level().isClientSide) {
            if (owner == null
                    || owner.isDeadOrDying()
                    || owner.getItemBySlot(EquipmentSlot.HEAD).getItem() != ModItems.GAZER_POD.get()) {

                // force stop
                this.setGazerState(GazerState.IDLE);
                if (owner != null && !owner.level().isClientSide) {
                    ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) owner),
                            new GazerCameraPacketS2C(this.getId(), false));
                }
            }
        }

        if (this.getGazerState() == GazerState.BEING_CONTROLLED && owner != null) {
            Inhabitants.LOGGER.debug("Server Control Tick");

            // raw targets from owner
            float playerYaw = owner.getYHeadRot();
            float playerPitch = owner.getXRot();

            // current values on this entity
            float currentYaw = this.getYHeadRot();
            float currentPitch = this.getXRot();

            // shortest yaw difference, smooth toward it, then wrap
            float yawDelta = Mth.wrapDegrees(playerYaw - currentYaw);
            float smoothYaw = Mth.wrapDegrees(currentYaw + yawDelta);

            // clamp and smooth pitch separately
            float clampedPitch = Mth.clamp(playerPitch, -90f, 90f);
            float pitchDelta = clampedPitch - currentPitch;
            float smoothPitch = currentPitch + pitchDelta;

            if ((this.yRotO < 0.0f && smoothYaw >= 0.0f) || (this.yRotO > 0.0f && smoothYaw <= 0.0f)) {
                // crossing zero, prevent interpolation issues
                float adjPrevYaw = this.yRotO;
                while (smoothYaw - adjPrevYaw > 180.0f) adjPrevYaw += 360.0f;
                while (smoothYaw - adjPrevYaw < -180.0f) adjPrevYaw -= 360.0f;

                this.yRotO = adjPrevYaw;
                this.yHeadRotO = adjPrevYaw;
                this.yBodyRotO = adjPrevYaw;
            }

            // apply rotations
            this.setYRot(smoothYaw);
            this.setYHeadRot(smoothYaw);
            this.setYBodyRot(smoothYaw);
            this.setXRot(smoothPitch);
        }

        // trigger RETURNING TO POD state
        if (this.getGazerState() == GazerState.IDLE && this.tickCount % 400 == 0 && !this.isEnteringPod()) {
            // find closest GazerPodEntity without a gazer
            this.setGazerState(GazerState.RETURNING_TO_POD);
        }

        // handle RETURNING TO POD state
        if (this.getGazerState() == GazerState.RETURNING_TO_POD && !this.isEnteringPod()) {
            if (returningPod == null || returningPod.isRemoved() || returningPod.hasGazer()) {
                // find closest GazerPodEntity without a gazer
                BlockPos nearestPodPos = GazerPodBlockRegistry.getNearestPodPosition(this.blockPosition());
                if (nearestPodPos != null) {
                    var be = level().getBlockEntity(nearestPodPos);
                    if (be instanceof GazerPodBlockEntity pod && !pod.hasGazer()) {
                        this.returningPod = pod;
                    }
                }

                if (returningPod != null) {
                    Vec3 center = returningPod.getBlockPos().getCenter();
                    this.getNavigation().moveTo(center.x, center.y, center.z, 1.0);
                } else {
                    this.setGazerState(GazerState.IDLE);
                }
            } else {
                // double check pod validity
                if (returningPod.isRemoved() || returningPod.hasGazer()) {
                    this.returningPod = null;
                    this.setGazerState(GazerState.IDLE);
                    return;
                }

                // Only recalculate if navigation is done
                Vec3 center = returningPod.getBlockPos().getCenter();

                if (this.getNavigation().isDone() && this.distanceToSqr(returningPod.getBlockPos().getCenter()) >= 4.0) {
                    this.getNavigation().moveTo(center.x, center.y, center.z, 1.0);
                }
                // If close enough, enter pod
                if (this.distanceToSqr(center) < 2.0 && !this.isEnteringPod()) {
                    this.setDeltaMovement(0, 0, 0);
                    this.enterPodWithBlock();
                }
            }
        }

        boolean podInvalid = returningPod == null || returningPod.isRemoved() || returningPod.hasGazer();
        boolean itemInvalid = returningPodItem == null || returningPodItem.isEmpty();

        if (this.isEnteringPod() && podInvalid && itemInvalid) {
            Inhabitants.LOGGER.debug("GazerEntity {} has no valid pod to enter, cancel enter", this.getUUID());
            this.setGazerState(GazerState.IDLE);
            this.setEnteringPod(false);
            podEntryTick = -1;
            returningPod = null;
            returningPodItem = ItemStack.EMPTY;
            return;
        }

        // handle pod entry discard timing
        if (!level().isClientSide && podEntryTick > 0 && this.tickCount - podEntryTick > 40) {

            if (returningPod != null && !returningPod.isRemoved() && !returningPod.hasGazer()) {
                Inhabitants.LOGGER.debug("GazerEntity {} entered pod block", this.getUUID());
                returningPod.setHasGazer(true);
            } else if (returningPodItem != null && returningPodItem.getItem() == ModItems.GAZER_POD.get() && !returningPodItem.isEmpty()) {
                Inhabitants.LOGGER.debug("GazerEntity {} returning to pod item in inventory", this.getUUID());
                GazerPodItem.setGazerId(returningPodItem, this.getUUID());
                GazerPodItem.setHasGazer(returningPodItem, true);
            }

            Inhabitants.LOGGER.debug("GazerEntity discarded");

            level().playSound(
                    null,
                    this.blockPosition(),
                    SoundEvents.BEEHIVE_ENTER,
                    SoundSource.BLOCKS,
                    1.0F, 1.0F
            );

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
    public void remove(@NotNull RemovalReason reason) {
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

    public void enterPodWithBlock() {

        this.setEnteringPod(true);

        podEntryTick = this.tickCount;
    }

    public void enterPodWithItem(ItemStack podItem) {

        this.setEnteringPod(true);

        podEntryTick = this.tickCount;

        if (podItem.getItem() instanceof GazerPodItem) {
            this.returningPodItem = podItem;
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
            return PlayState.CONTINUE;
        }

        // anim when gazer exits pod
        if (this.tickCount < 20 && this.podEntryTick == -1) {
            controller.setAnimation(RawAnimation.begin().then("exit pod", Animation.LoopType.PLAY_ONCE));
            Inhabitants.LOGGER.debug("Playing take off animation");
            return PlayState.CONTINUE;
        }

        if (this.getDeltaMovement().length() > 0.01) {
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

    @Override
    public void die(@NotNull DamageSource cause) {
        super.die(cause);

        if (!level().isClientSide && this.getGazerState() == GazerState.BEING_CONTROLLED) {
            this.setGazerState(GazerState.IDLE);

            UUID ownerUUID = this.getOwnerUUID();
            if (ownerUUID != null) {
                Player player = this.level().getPlayerByUUID(ownerUUID);
                if (player instanceof ServerPlayer serverPlayer) {
                    ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new GazerCameraPacketS2C(this.getId(), false));
                }
            }
        }

        setOwnerUUID(null);
    }
}
