package com.jeremyseq.inhabitants.entities.gazer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class GazerEntity extends FlyingMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GazerState currentState = GazerState.IDLE;
    public UUID podOwner;

    public GazerEntity(EntityType<? extends FlyingMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.noPhysics = true;
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
        INSIDE_POD,         // Hidden inside a pod block/item (not visible in world)
        IDLE,               // Floating near the pod
        FOLLOWING_PLAYER,   // Following the player with the empty pod item
        BEING_CONTROLLED,   // Player is “possessing” the gazer
        RETURNING           // Pathfinding back to pod or player’s pod item
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
        // do nothing, bat doesn’t take fall damage
    }

    @Override
    protected void registerGoals() {
        // follow player holding pod item when FOLLOWING_PLAYER
        this.goalSelector.addGoal(1, new FollowPodHolderGoal(this));

        // Random floating movement when IDLE
        this.goalSelector.addGoal(2, new GazerWanderGoal(this, 5.0D));

        // Look around when IDLE
        this.goalSelector.addGoal(3, new GazerLookAroundGoal(this));
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level pLevel) {
        return new FlyingPathNavigation(this, pLevel);
    }

    @Override
    public void tick() {
        super.tick();

        switch (currentState) {
//            case IDLE -> handleIdle();
//            case FOLLOWING_PLAYER -> handleFollowingPlayer();
//            case BEING_CONTROLLED -> handleBeingControlled();
            case RETURNING -> handleReturning();
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
    }


    // ----- Behavior Handlers -----

//    private void handleIdle() {
//        // Float lazily near pod
//
//    }
//
//    private void handleFollowingPlayer() {
//        if (podOwner == null) return;
//        Player player = this.level().getPlayerByUUID(podOwner);
//        if (player != null) {
//            this.getNavigation().moveTo(player, 1.0D);
//        }
//    }
//
//    private void handleBeingControlled() {
//    }

    private void handleReturning() {
        // Path back to pod block or pod item in player’s hand
    }

    // ----- State Transitions -----

    public void enterPod() {
        this.currentState = GazerState.INSIDE_POD;
        this.discard(); // remove from world
    }

    public void exitPod(Player owner, boolean controlled) {
        this.currentState = controlled ? GazerState.BEING_CONTROLLED : GazerState.IDLE;
        this.podOwner = owner.getUUID();
    }

    public void startFollowing(Player owner) {
        this.currentState = GazerState.FOLLOWING_PLAYER;
        this.podOwner = owner.getUUID();
    }

    public void startReturning() {
        this.currentState = GazerState.RETURNING;
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
