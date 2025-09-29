package com.jeremyseq.inhabitants.entities.gazer;

import com.jeremyseq.inhabitants.Inhabitants;
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

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class GazerEntity extends FlyingMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Random random = new Random();

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
        FlyingPathNavigation flyingpathnavigation = new FlyingPathNavigation(this, pLevel);
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
        if (this.getGazerState() == GazerState.IDLE && random.nextInt(100) == 0) {
            // find closest GazerPodEntity without a gazer
            this.setGazerState(GazerState.RETURNING_TO_POD);
        }

        // handle RETURNING TO POD state
        if (this.getGazerState() == GazerState.RETURNING_TO_POD) {
            if (returningPod == null || returningPod.isRemoved() || returningPod.hasGazer()) {
                // find closest GazerPodEntity without a gazer
                this.returningPod = findNearestAvailablePod(50);

                if (returningPod != null) {
                    Vec3 center = returningPod.getBlockPos().getCenter();
                    this.getNavigation().moveTo(center.x, center.y, center.z, 1.0);
                } else {
                    this.setGazerState(GazerState.IDLE);
                }
            } else {
                // Only recalculate if navigation is done
                Vec3 center = returningPod.getBlockPos().getCenter();

                if (this.getNavigation().isDone() && this.distanceToSqr(returningPod.getBlockPos().getCenter()) >= 4.0) {
                    this.getNavigation().moveTo(center.x, center.y, center.z, 1.0);
//                    Inhabitants.LOGGER.debug("GazerEntity {} recalculating path to pod {}", this.getUUID(), returningPod.getUUID());
                }
                // If close enough, enter pod
                if (this.distanceToSqr(center) < 4.0 && !this.isEnteringPod()) {
//                    Inhabitants.LOGGER.debug("GazerEntity {} entering pod {}", this.getUUID(), returningPod.getUUID());
                    this.setDeltaMovement(0, 0, 0);
                    this.enterPodWithBlock();
                }
            }
        }

        // handle pod entry discard timing
        if (!level().isClientSide && podEntryTick > 0 && this.tickCount - podEntryTick > 40) {
            if (returningPod != null && !returningPod.isRemoved() && !returningPod.hasGazer()) {
//                Inhabitants.LOGGER.debug("GazerEntity {} entered pod entity {}", this.getUUID(), returningPod.getUUID());
                returningPod.setHasGazer(true);
            } else if (returningPodItem != null && returningPodItem.getItem() == ModItems.GAZER_POD.get() && !returningPodItem.isEmpty()) {
                // drop pod item if no pod entity to return to
                Inhabitants.LOGGER.debug("GazerEntity {} returning to pod item in inventory", this.getUUID());
                GazerPodItem.setGazerId(returningPodItem, this.getUUID());
                GazerPodItem.setHasGazer(returningPodItem, true);
            }
            Inhabitants.LOGGER.debug("GazerEntity discarded");
            this.discard();
        }

        Inhabitants.LOGGER.debug("tickCount: {} podEntryTick: {} isEnteringPod: {}", this.tickCount, podEntryTick, this.isEnteringPod());
    }

    private GazerPodBlockEntity findNearestAvailablePod(double radius) {
        BlockPos center = this.blockPosition();
        GazerPodBlockEntity closestPod = null;
        double closestDistance = Double.MAX_VALUE;

        // Only check loaded chunks in a cube around the entity
        int intRadius = (int)Math.ceil(radius);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-intRadius, -intRadius, -intRadius), center.offset(intRadius, intRadius, intRadius))) {
            if (level().isLoaded(pos)) {
                var be = level().getBlockEntity(pos);
                if (be instanceof GazerPodBlockEntity pod && !pod.hasGazer()) {
                    double distance = this.position().distanceTo(Vec3.atCenterOf(pos));
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestPod = pod;
                    }
                }
            }
        }
        return closestPod;
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
