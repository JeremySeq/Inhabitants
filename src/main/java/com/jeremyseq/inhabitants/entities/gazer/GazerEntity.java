package com.jeremyseq.inhabitants.entities.gazer;

import com.jeremyseq.inhabitants.items.GazerPodItem;
import com.jeremyseq.inhabitants.items.ModItems;
import com.jeremyseq.inhabitants.networking.GazerCameraPacketS2C;
import com.jeremyseq.inhabitants.networking.ModNetworking;
import net.minecraft.core.BlockPos;
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
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class GazerEntity extends FlyingMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GazerState currentState = GazerState.IDLE;
    public UUID podOwner = null;

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
        // follow player holding pod item when IDLE
//        this.goalSelector.addGoal(1, new FollowPodHolderGoal(this));

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

        // if being controlled, check owner validity
        if (currentState == GazerState.BEING_CONTROLLED) {
            ServerPlayer owner = (ServerPlayer) this.level().getPlayerByUUID(podOwner);
            if (owner == null
                    || owner.isDeadOrDying()
                    || owner.getItemBySlot(EquipmentSlot.HEAD).getItem() != ModItems.GAZER_POD.get()) {

                // force stop
                currentState = GazerState.IDLE;
                podOwner = null;

                if (owner != null && !owner.level().isClientSide) {
                    ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> owner),
                            new GazerCameraPacketS2C(getId(), false));
                }
            }
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

        if (podOwner == null) return;
        Player player = this.level().getPlayerByUUID(podOwner);
        if (player == null) return;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof GazerPodItem) {
                if (GazerPodItem.getGazerId(stack) == this.getId()) {
                    GazerPodItem.setGazerId(stack, -1);
                }
            }
        }
    }

    // ----- State Transitions -----

    public void enterPod() {
        this.discard();
    }

    public void exitPod(Player player, boolean controlled) {
        this.currentState = controlled ? GazerState.BEING_CONTROLLED : GazerState.IDLE;
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
