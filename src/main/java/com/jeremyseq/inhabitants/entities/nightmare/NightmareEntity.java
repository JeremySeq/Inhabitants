package com.jeremyseq.inhabitants.entities.nightmare;

import com.jeremyseq.inhabitants.effects.ModEffects;
import com.jeremyseq.inhabitants.entities.ModEntities;
import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

import java.util.Random;

public class NightmareEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public static final EntityDataAccessor<Integer> TEXTURE = SynchedEntityData.defineId(NightmareEntity.class, EntityDataSerializers.INT);

    public static final int INITIAL_DEATH_ANIM_TICKS = 86;

    public int dyingTicks = 0;

    public NightmareEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    public NightmareEntity(Level pLevel) {
        super(ModEntities.NIGHTMARE.get(), pLevel);
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    protected @NotNull PathNavigation createNavigation(@NotNull Level pLevel) {
        FlyingPathNavigation flyingpathnavigation = new FlyingPathNavigation(this, pLevel);
        flyingpathnavigation.setCanOpenDoors(false);
        flyingpathnavigation.setCanFloat(true);
        flyingpathnavigation.setCanPassDoors(true);
        return flyingpathnavigation;
    }

    public static AttributeSupplier setAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30f)
                .add(Attributes.ATTACK_DAMAGE, 8f)
                .add(Attributes.ATTACK_SPEED, 1.0f)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5F)
                .add(Attributes.FOLLOW_RANGE, 30f)
                .add(Attributes.FLYING_SPEED, .3f).build();
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity pEntity) {
        if (!level().isClientSide) {
            this.triggerAnim("attack", "melee");
        }
        // random chance to apply panic effect on hit
        if (random.nextFloat() < 0.5f) {
            if (pEntity instanceof LivingEntity livingEntity) {
                livingEntity.addEffect(new MobEffectInstance(ModEffects.PANIC.get(), 200, 0));
            }
        }
        return super.doHurtTarget(pEntity);
    }

    @Override
    public void tickDeath() {
        ++this.dyingTicks;

        if (this.dyingTicks == 1) {
            this.triggerAnim("death", "death_initial");
        }

        // only start dropping entity after initial death anim is done; initial death anim moves nightmare downwards anyway
        if (this.dyingTicks >= INITIAL_DEATH_ANIM_TICKS) {
            // spiral down: fall downwards while moving forwards and rotating
            Vec3 normal = Vec3.atCenterOf(this.getDirection().getNormal());
            this.setDeltaMovement(new Vec3(0, -0.1, 0).add(normal.scale(0.05)));
            this.setYRot(this.getYRot() + 2);
            // trigger looping drop anim after initial death anim
            this.triggerAnim("death", "death_looping");
        } else {
            // cancel out movement during intial death anim
            this.setDeltaMovement(new Vec3(0, 0, 0));
        }

        // actually kill entity when it hits the ground and has played initial death anim
        if (this.onGround() && this.dyingTicks >= INITIAL_DEATH_ANIM_TICKS) {
            this.remove(Entity.RemovalReason.KILLED);
            this.playSound(SoundEvents.ITEM_PICKUP);
            this.spawnAtLocation(new ItemStack(ModItems.DREAD_CLOTH.get(), new Random().nextInt(1, 2)));
        }
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, @NotNull BlockState state, @NotNull BlockPos pos) {
        // no fall damage
    }

    public int getTexture() {
        return entityData.get(TEXTURE);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(TEXTURE, random.nextInt(0, 4));
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("texture", entityData.get(TEXTURE));
        tag.putBoolean("dying", dead);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("texture")) {
            entityData.set(TEXTURE, tag.getInt("texture"));
        }

        if (tag.contains("dying")) {
            dead = tag.getBoolean("dying");
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 10, this::predicate));
        controllers.add(new AnimationController<>(this, "attack", 0, state -> PlayState.STOP)
                .triggerableAnim("melee", RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE)));
        controllers.add(new AnimationController<>(this, "death", 0, state -> PlayState.STOP)
                .triggerableAnim("death_initial", RawAnimation.begin().then("death_initial", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("death_looping", RawAnimation.begin().then("death_looping", Animation.LoopType.LOOP)));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> animationState) {
        if (animationState.isMoving()) {
            animationState.getController().setAnimation(RawAnimation.begin().then("flying", Animation.LoopType.LOOP));
        } else {
            animationState.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
        }

        return PlayState.CONTINUE;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this , Player.class, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
