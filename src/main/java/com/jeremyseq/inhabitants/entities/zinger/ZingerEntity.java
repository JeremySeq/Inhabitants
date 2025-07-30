package com.jeremyseq.inhabitants.entities.zinger;

import com.jeremyseq.inhabitants.entities.goals.ConditionalStrollGoal;
import com.jeremyseq.inhabitants.gui.ZingerChestMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

import javax.annotation.Nullable;
import java.util.*;

public class ZingerEntity extends PathfinderMob implements GeoEntity {

    private static final TicketType<ZingerEntity> ZINGER_TICKET = TicketType.create(
            "zinger",
            Comparator.comparingInt(System::identityHashCode)
    );

    private static final int CHUNK_RADIUS = 1; // 1 = 3x3 area

    protected boolean returningToNest = false;
    private ChunkPos lastCenterChunk;

    public static final EntityDataAccessor<String> FLIGHT_STATE = SynchedEntityData.defineId(ZingerEntity.class, EntityDataSerializers.STRING);

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    private final SimpleContainer chestInventory = new SimpleContainer(27); // 3x9

    private UUID ownerUUID;
    private boolean hasChest = false;

    public enum FlightState {
        GROUNDED,
        TAKING_OFF,
        FLYING,
        LANDING,
        HOVERING
    }

    @Nullable
    private Vec3 targetPosition;

    public Vec3 nestPosition = new Vec3(0, 140, 0);

    public ZingerEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.moveControl = new FlyingMoveControl(this, 20, false);
        this.setPersistenceRequired();
    }

    public boolean isAtNest() {
        if (this.nestPosition == null) return false;
        return this.position().distanceToSqr(nestPosition) < 10;
    }

    @Override
    public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
        return false;
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50f)
                .add(Attributes.ATTACK_DAMAGE, 15f)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5F)
                .add(Attributes.FOLLOW_RANGE, 30f)
                .add(Attributes.FLYING_SPEED, 2f)
                .add(Attributes.MOVEMENT_SPEED, .25f).build();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new ZingerReturnToNestGoal(this));
        this.goalSelector.addGoal(3, new ConditionalStrollGoal<>(this, 1, (zingerEntity -> !zingerEntity.isFlying())));
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level pLevel) {
        return new GroundPathNavigation(this, pLevel);
    }

    public boolean isFlying() {
        return this.getFlightState() != FlightState.GROUNDED;
    }

    public void triggerReturnToNest() {
        this.setTarget(null);
        this.returningToNest = true;
    }

    public void triggerFlyToOwner() {
        if (this.getOwnerUUID() != null) {
            Player owner = this.level().getPlayerByUUID(this.getOwnerUUID());
            if (owner != null) {
                this.returningToNest = false;
                this.setTarget(owner);
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> animationState) {
        if (this.isFlying()) {
            animationState.getController().setAnimation(RawAnimation.begin().then("flying_idle", Animation.LoopType.LOOP));
        } else {
            animationState.getController().setAnimation(RawAnimation.begin().then("idle_ground", Animation.LoopType.LOOP));
        }

        return PlayState.CONTINUE;
    }

    public Container getChestInventory() {
        return chestInventory;
    }

    @Override
    public @NotNull InteractionResult interactAt(@NotNull Player pPlayer, @NotNull Vec3 pVec, @NotNull InteractionHand pHand) {
        if (this.level().isClientSide) return super.interactAt(pPlayer, pVec, pHand);

        if (pPlayer.isShiftKeyDown()) {

            // open chest inventory
            if (this.hasChest) {
                NetworkHooks.openScreen((ServerPlayer) pPlayer, new SimpleMenuProvider(
                        (id, playerInv, player) -> new ZingerChestMenu(id, playerInv, this.chestInventory),
                        Component.literal("Zinger Chest")
                ));
            }
            return InteractionResult.SUCCESS;
        }

        if (pPlayer.getUUID().equals(this.getOwnerUUID()) && pHand == InteractionHand.MAIN_HAND) {
            if (pPlayer.getItemInHand(pHand).is(Items.CHEST)) {
                if (!this.hasChest) {
                    this.hasChest = true;
                    pPlayer.getItemInHand(pHand).shrink(1);
                }
                return InteractionResult.SUCCESS;
            }

            if (pPlayer.startRiding(this, true)) {
                this.setTarget(null); // clear target when mounting
                triggerReturnToNest();
                return InteractionResult.SUCCESS;
            }
        }
        return super.interactAt(pPlayer, pVec, pHand);
    }

    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull MoveFunction pCallback) {
        if (this.hasPassenger(passenger)) {
            double offsetY = -.4f;
            float yawRad = (float) Math.toRadians(this.getYRot());

            double xOffset = -Math.sin(yawRad) * 0.3;
            double zOffset = Math.cos(yawRad) * 0.3;

            passenger.setPos(
                    this.getX() + xOffset,
                    this.getY() + offsetY,
                    this.getZ() + zOffset
            );
        }
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    private void handleChunkLoadingTick() {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            ChunkPos currentCenter = new ChunkPos(this.blockPosition());

            if (lastCenterChunk == null || !lastCenterChunk.equals(currentCenter)) {
                // unload old surrounding chunks
                if (lastCenterChunk != null) {
                    for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
                        for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                            ChunkPos pos = new ChunkPos(lastCenterChunk.x + dx, lastCenterChunk.z + dz);
                            serverLevel.getChunkSource().removeRegionTicket(ZINGER_TICKET, pos, 2, this);
                        }
                    }
                }

                // load new surrounding chunks
                for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
                    for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                        ChunkPos pos = new ChunkPos(currentCenter.x + dx, currentCenter.z + dz);
                        serverLevel.getChunkSource().addRegionTicket(ZINGER_TICKET, pos, 2, this);
                    }
                }

                lastCenterChunk = currentCenter;
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.handleChunkLoadingTick();

        // just for testing, set owner to "Dev"
        List<? extends Player> players = this.level().players();
        if (!players.isEmpty()) {
            setOwnerUUID(players.get(0).getUUID());
        }

        LivingEntity target = this.getTarget();
        Vec3 customTarget = this.getTargetPosition();

        boolean hasTarget = target != null;
        boolean hasCustomTarget = customTarget != null;

        Vec3 destination = null;


        if (hasCustomTarget) {
            destination = customTarget;
        } else if (hasTarget) {
            destination = target.position().add(0, 2, 0);
        }

        switch (getFlightState()) {
            case GROUNDED -> {
                if (destination != null) {
                    setFlightState(FlightState.TAKING_OFF);
                    this.setNoGravity(true);
                }
            }
            case TAKING_OFF -> {
                handleTakeoff();
            }
            case HOVERING -> {
                if (destination == null) {
                    setFlightState(FlightState.LANDING);
                } else {
                    double xyDist = this.getXZDistanceTo(destination);
                    if (xyDist > 12) {
                        setFlightState(FlightState.FLYING);
                    } else {
                        handleHovering(destination);
                    }
                }
            }
            case FLYING -> {
                if (destination == null) {
                    setFlightState(FlightState.LANDING);
                } else {
                    double dist = this.getXZDistanceTo(destination);
                    if (dist < 10) {
                        setFlightState(FlightState.HOVERING);
                    } else {
                        handleFlying(new Vec2((float) destination.x, (float) destination.z));
                    }
                }
            }
            case LANDING -> {
                handleLanding();
                if (this.onGround()) {
                    setFlightState(FlightState.GROUNDED);
                    this.setNoGravity(false);
                    this.setDeltaMovement(Vec3.ZERO);
                }
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource pSource, int pLooting, boolean pRecentlyHit) {
        super.dropCustomDeathLoot(pSource, pLooting, pRecentlyHit);
        if (this.hasChest) {
            // drop the chest contents
            for (int i = 0; i < this.chestInventory.getContainerSize(); i++) {
                ItemStack stack = this.chestInventory.getItem(i);
                if (!stack.isEmpty()) {
                    this.spawnAtLocation(stack);
                }
            }
            this.chestInventory.clearContent();
        }
    }

    private double getXZDistanceTo(Vec3 targetPos) {
        return Math.sqrt(Math.pow(this.getX() - targetPos.x, 2) + Math.pow(this.getZ() - targetPos.z, 2));
    }
    private double getXZDistanceTo(Vec2 targetPos) {
        return Math.sqrt(Math.pow(this.getX() - targetPos.x, 2) + Math.pow(this.getZ() - targetPos.y, 2));
    }

    private void handleFlying(Vec2 targetXZ) {
        double forwardSpeed = 1.5;
        double turnRate = 8.0; // degrees per tick
        double climbRate = 1; // controls vertical smoothing

        double targetX = targetXZ.x;
        double targetZ = targetXZ.y;
        double targetY = this.getFlyHeight();

        // compute desired yaw to face the target position
        double dx = targetX - this.getX();
        double dz = targetZ - this.getZ();
        float desiredYaw = (float)(Math.atan2(dz, dx) * (180 / Math.PI)) - 90;

        // smoothly rotate toward the target yaw
        float currentYaw = this.getYRot();
        float newYaw = rotateTowards(currentYaw, desiredYaw, (float) turnRate);
        this.setYRot(newYaw);
        this.yBodyRot = newYaw;
        this.yHeadRot = newYaw;

        // compute vertical adjustment
        double dy = (targetY) - (this.getY() + this.getBbHeight() / 2);
        double verticalMotion = dy * climbRate;
        verticalMotion = Math.max(Math.min(verticalMotion, 0.3), -0.3); // clamp

        // fly forward in the direction we're facing
        Vec3 forward = Vec3.directionFromRotation(0, newYaw).scale(forwardSpeed);
        Vec3 motion = new Vec3(forward.x, verticalMotion, forward.z);
        this.setDeltaMovement(motion);

        this.getLookControl().setLookAt(targetX, targetY, targetZ, 10, 10);
    }

    private float rotateTowards(float current, float target, float maxChange) {
        float delta = Mth.wrapDegrees(target - current);
        if (delta > maxChange) delta = maxChange;
        if (delta < -maxChange) delta = -maxChange;
        return current + delta;
    }

    private void handleLanding() {
        double landingSpeed = .25; // descent speed

        // descend smoothly
        var currentMotion = this.getDeltaMovement();
        var descendMotion = new Vec3(currentMotion.x, -landingSpeed, currentMotion.z);

        this.setDeltaMovement(currentMotion.add(descendMotion.subtract(currentMotion).scale(0.1)));

        // look forward while landing
        var lookVec = this.getLookAngle().normalize();
        this.getLookControl().setLookAt(this.getX() + lookVec.x, this.getY(), this.getZ() + lookVec.z, 10, 10);

        // check if on or near ground to finish landing
        if (this.onGround()) {
            setFlightState(FlightState.GROUNDED);
            this.setNoGravity(false);
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    private void handleHovering(Vec3 targetPos) {

        double hoverX = targetPos.x;
        double hoverY = targetPos.y;
        double hoverZ = targetPos.z;

        Vec3 currentPos = this.position();

        // Compute deltas
        double dx = hoverX - currentPos.x;
        double dy = hoverY - currentPos.y;
        double dz = hoverZ - currentPos.z;

        // Smooth motion toward target position
        double horizontalSpeed = 0.15;
        double verticalSpeed = .4;

        dx = Mth.clamp(dx * 0.3, -horizontalSpeed, horizontalSpeed);
        dy = Mth.clamp(dy * 0.3, -verticalSpeed, verticalSpeed);
        dz = Mth.clamp(dz * 0.3, -horizontalSpeed, horizontalSpeed);

        this.setDeltaMovement(dx, dy, dz);
    }


    private void handleTakeoff() {
        double ascendSpeed = 1.5;
        double forwardSpeed = .5;

        var lookVec = this.getLookAngle().normalize();
        var horizontalLook = lookVec.multiply(1, 0, 1).normalize();
        var ascendMotion = horizontalLook.scale(forwardSpeed).add(0, ascendSpeed, 0);

        var currentMotion = this.getDeltaMovement();
        var newMotion = currentMotion.add(ascendMotion.subtract(currentMotion).scale(0.1));
        this.setDeltaMovement(newMotion);

        this.getLookControl().setLookAt(this.getX() + lookVec.x, this.getY() + ascendSpeed, this.getZ() + lookVec.z, 10, 10);

        // Check if reached target altitude, then switch to flying
        if (this.getY() >= getFlyHeight()) {
            setFlightState(FlightState.FLYING);
        }
    }

    private int getFlyHeight() {
        return this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) this.getX(), (int) this.getZ()) + 100;
    }


    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, @NotNull DamageSource source) {
        // ignore fall damage entirely
        return false;
    }

    private void setFlightState(FlightState state) {
        this.entityData.set(FLIGHT_STATE, state.name());
    }

    public FlightState getFlightState() {
        return FlightState.valueOf(this.entityData.get(FLIGHT_STATE));
    }

    public void setTargetPosition(@Nullable Vec3 pos) {
        this.targetPosition = pos;
    }

    @Nullable
    public Vec3 getTargetPosition() {
        return this.targetPosition;
    }

    /**
     * @return the UUID of the player who owns this Zinger.
     */
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    private void setOwnerUUID(UUID ownerUUID) {
        if (!Objects.equals(this.ownerUUID, ownerUUID)) {
            ZingerManager.unregisterZinger(this);
            this.ownerUUID = ownerUUID;
            ZingerManager.registerZinger(this);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FLIGHT_STATE, FlightState.GROUNDED.name());
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(reason);

        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel && lastCenterChunk != null) {
            for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
                for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                    ChunkPos pos = new ChunkPos(lastCenterChunk.x + dx, lastCenterChunk.z + dz);
                    serverLevel.getChunkSource().removeRegionTicket(ZINGER_TICKET, pos, 2, this);
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        if (this.getOwnerUUID() != null) {
            pCompound.putUUID("OwnerUUID", this.getOwnerUUID());
        }
        pCompound.putBoolean("hasChest", this.hasChest);

        // saves the chunk pos to be loaded in later
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            ChunkPos pos = new ChunkPos(this.blockPosition());
            ZingerChunkData.get(serverLevel).addZingerChunk(pos);
        }
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        ZingerManager.registerZinger(this);
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        ZingerManager.unregisterZinger(this);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);

        if (pCompound.contains("OwnerUUID")) {
            setOwnerUUID(pCompound.getUUID("OwnerUUID"));
        } else {
            setOwnerUUID(null);
        }

        if (pCompound.contains("hasChest")) {
            this.hasChest = pCompound.getBoolean("hasChest");
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
