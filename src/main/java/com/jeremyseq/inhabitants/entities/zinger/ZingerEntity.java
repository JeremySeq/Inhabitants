package com.jeremyseq.inhabitants.entities.zinger;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.entities.goals.ConditionalStrollGoal;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

import java.util.List;

public class ZingerEntity extends PathfinderMob implements GeoEntity {
    public static final EntityDataAccessor<String> FLIGHT_STATE = SynchedEntityData.defineId(ZingerEntity.class, EntityDataSerializers.STRING);

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    public enum FlightState {
        GROUNDED,
        TAKING_OFF,
        FLYING,
        LANDING,
        HOVERING
    }

    private static final int HOVER_HEIGHT = 10;

    private double takeoffYTarget = -1;

    public ZingerEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.moveControl = new FlyingMoveControl(this, 20, false);
        this.setPersistenceRequired();
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
        this.goalSelector.addGoal(1, new ConditionalStrollGoal<>(this, 1, (zingerEntity -> !zingerEntity.isFlying())));
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level pLevel) {
        return new GroundPathNavigation(this, pLevel);
    }

    public boolean isFlying() {
        return this.getFlightState() != FlightState.GROUNDED;
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

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        // Find player named "Dev"
        Player devPlayer = null;
        List<? extends Player> players = this.level().players();
        for (Player player : players) {
            if ("Dev".equals(player.getName().getString())) {
                devPlayer = player;
                break;
            }
        }

        if (devPlayer != null && !devPlayer.isCreative()) {
            this.setTarget(devPlayer);
        } else {
            this.setTarget(null);
        }

        var target = this.getTarget();
        boolean hasTarget = target != null;

        switch (getFlightState()) {
            case GROUNDED -> {
                if (hasTarget) {
                    setFlightState(FlightState.TAKING_OFF);
                    this.setNoGravity(true);
                    takeoffYTarget = this.getY() + HOVER_HEIGHT;
                }
            }
            case TAKING_OFF -> {
                handleTakeoff();
            }
            case HOVERING -> {
                if (!hasTarget) {
                    setFlightState(FlightState.LANDING);
                } else {
                    Vec3 desiredHoverPos = target.position().add(0, HOVER_HEIGHT, 0);
                    double dist = this.position().distanceTo(desiredHoverPos);
                    if (dist > 12) {
                        setFlightState(FlightState.FLYING);
                    } else {
                        handleHovering(target);
                    }
                }
            }
            case FLYING -> {
                if (!hasTarget) {
                    setFlightState(FlightState.LANDING);
                } else {
                    Vec3 hoverTarget = target.position().add(0, HOVER_HEIGHT, 0);

                    // close enough to hover
                    if (Math.abs(this.getY() - hoverTarget.y) < 1.0 &&
                            this.position().distanceTo(hoverTarget) < 10) {
                        setFlightState(FlightState.HOVERING);
                    } else {
                        handleFlying(hoverTarget);
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
        Inhabitants.LOGGER.debug(String.valueOf(this.getFlightState()));
    }

    private void handleFlying(Vec3 targetPos) {
        double forwardSpeed = .6;
        double turnRate = 8.0; // degrees per tick
        double climbRate = 1; // controls vertical smoothing

        if (targetPos != null) {
            // compute desired yaw to face the target position
            double dx = targetPos.x - this.getX();
            double dz = targetPos.z - this.getZ();
            float desiredYaw = (float)(Math.atan2(dz, dx) * (180 / Math.PI)) - 90;

            // smoothly rotate toward the target yaw
            float currentYaw = this.getYRot();
            float newYaw = rotateTowards(currentYaw, desiredYaw, (float) turnRate);
            this.setYRot(newYaw);
            this.yBodyRot = newYaw;
            this.yHeadRot = newYaw;

            // compute vertical adjustment
            double dy = (targetPos.y) - (this.getY() + this.getBbHeight() / 2);
            double verticalMotion = dy * climbRate;
            verticalMotion = Math.max(Math.min(verticalMotion, 0.3), -0.3); // clamp

            // fly forward in the direction we're facing
            Vec3 forward = Vec3.directionFromRotation(0, newYaw).scale(forwardSpeed);
            Vec3 motion = new Vec3(forward.x, verticalMotion, forward.z);
            this.setDeltaMovement(motion);

            this.getLookControl().setLookAt(targetPos.x, targetPos.y, targetPos.z, 10, 10);
        } else {
            // if no target, glide forward with no vertical movement
            Vec3 forward = Vec3.directionFromRotation(0, this.getYRot()).scale(forwardSpeed);
            this.setDeltaMovement(new Vec3(forward.x, this.getDeltaMovement().y * 0.9, forward.z));
        }
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

    private void handleHovering(Entity target) {
        if (target == null) return;

        Vec3 targetPos = target.position();
        double hoverX = targetPos.x;
        double hoverY = targetPos.y + HOVER_HEIGHT;
        double hoverZ = targetPos.z;

        Vec3 currentPos = this.position();

        // Compute deltas
        double dx = hoverX - currentPos.x;
        double dy = hoverY - currentPos.y;
        double dz = hoverZ - currentPos.z;

        // Smooth motion toward target position
        double horizontalSpeed = 0.1;
        double verticalSpeed = 0.1;

        dx = Mth.clamp(dx * 0.3, -horizontalSpeed, horizontalSpeed);
        dy = Mth.clamp(dy * 0.3, -verticalSpeed, verticalSpeed);
        dz = Mth.clamp(dz * 0.3, -horizontalSpeed, horizontalSpeed);

        this.setDeltaMovement(dx, dy, dz);

        // Keep looking at target
        this.getLookControl().setLookAt(target.getX(), target.getY(), target.getZ(), 10, 10);
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
        if (this.getY() >= takeoffYTarget - 0.5) {
            setFlightState(FlightState.FLYING);
        }
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

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FLIGHT_STATE, FlightState.GROUNDED.name());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
