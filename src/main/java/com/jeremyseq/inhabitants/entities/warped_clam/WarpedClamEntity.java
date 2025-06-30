package com.jeremyseq.inhabitants.entities.warped_clam;

import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

import java.util.List;

public class WarpedClamEntity extends Mob implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public static final EntityDataAccessor<Boolean> OPEN = SynchedEntityData.defineId(WarpedClamEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> FLING_ANIM = SynchedEntityData.defineId(WarpedClamEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> HAS_PEARL = SynchedEntityData.defineId(WarpedClamEntity.class, EntityDataSerializers.BOOLEAN);

    private int pearlRegenTimer = 0; // Ticks until pearl regenerates
    private int launchDelayTicks = 0;
    private int popDelayTicks = 0;
    private boolean lastOpenState = false;

    public WarpedClamEntity(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20f).build();
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor pLevel,
                                                  @NotNull DifficultyInstance pDifficulty,
                                                  @NotNull MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData,
                                                  @Nullable CompoundTag pDataTag) {
        if ((pReason == MobSpawnType.SPAWN_EGG) && pLevel.getNearestPlayer(this, 10) != null) {
            // face same direction as nearest player within 10 blocks (usually the one who used the egg)
            Player player = pLevel.getNearestPlayer(this, 10);
            if (player != null) {
                float playerYaw = player.getYRot();
                this.setYRot(playerYaw);
                this.setYHeadRot(playerYaw);
                this.setYBodyRot(playerYaw);
            }
        } else {
            // for natural spawn or summon, randomize direction
            float yaw = pLevel.getRandom().nextFloat() * 360f;
            this.setYRot(yaw);
            this.setYHeadRot(yaw);
            this.setYBodyRot(yaw);
        }

        return pSpawnData;
    }

    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        if (!this.isOpen() && pSource.getEntity() instanceof LivingEntity) {
            return false;
        }
        return super.hurt(pSource, pAmount);
    }

    @Override
    public void die(@NotNull DamageSource pDamageSource) {
        if (this.hasPearl()) {
            popPearl();
        }
    }

    @Override
    public void knockback(double pStrength, double pX, double pZ) {}

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();

        // regen pearl
        if (!hasPearl() && pearlRegenTimer > 0) {
            pearlRegenTimer--;
            if (pearlRegenTimer <= 0) {
                setHasPearl(true);
            }
        }

        // Check for players standing on top and trigger launch delay
        if (!level().isClientSide && launchDelayTicks == 0) {
            AABB topBox = getBoundingBox().move(0, 0.4, 0); // slightly above the clam
            List<LivingEntity> entities = level().getEntitiesOfClass(LivingEntity.class, topBox);
            entities.remove(this);

            if (!entities.isEmpty()) {
                entityData.set(FLING_ANIM, false);
                entityData.set(FLING_ANIM, true);
                launchDelayTicks = 8;
            }
        }

        // Handle launch delay
        if (launchDelayTicks > 0) {
            launchDelayTicks--;
            if (launchDelayTicks == 0) {
                launchEntity();
            }
        }

        // handle pearl pop delay
        if (popDelayTicks > 0) {
            popDelayTicks--;
            if (popDelayTicks == 0) {
                entityData.set(OPEN, false);
            }
        }

        // particles if clam has pearl
        if (hasPearl() && level().isClientSide && level().random.nextFloat() < 0.1f) {
            for (int i = 0; i < 8; i++) {
                double x = getX() + (random.nextDouble() - 0.5);
                double y = getY() + 0.7 + random.nextDouble() * 0.4;
                double z = getZ() + (random.nextDouble() - 0.5);

                double dx = (random.nextDouble() - 0.5) * 0.02;
                double dy = random.nextDouble() * 0.01;
                double dz = (random.nextDouble() - 0.5) * 0.02;

                level().addParticle(ParticleTypes.PORTAL, x, y, z, dx, dy, dz);
            }
        }
    }

    @Override
    protected @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        ItemStack item = player.getItemInHand(hand);

        if (item.getItem() instanceof ShovelItem) {
            item.hurtAndBreak(3, player, (p) -> p.broadcastBreakEvent(hand));
            if (!level().isClientSide) {
                this.discard();
                ItemStack clamItem = new ItemStack(ModItems.WARPED_CLAM_ITEM.get());
                clamItem.getOrCreateTag().putBoolean("has_pearl", hasPearl());
                this.spawnAtLocation(clamItem);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        if (!isOpen() && item.is(Items.BRUSH)) {
            if (!level().isClientSide) {
                entityData.set(OPEN, true);
                popDelayTicks = 60; // how long clam stays open

                level().playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_CHEST_OPEN, SoundSource.NEUTRAL, 3.0f, 1.0f);

                // play the brushing sound
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.BRUSH_SAND, SoundSource.PLAYERS, 1.0f, 1.0f);

                // spawn brushing particles like suspicious sand
                ((ServerLevel) level()).sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SUSPICIOUS_SAND.defaultBlockState()),
                        getX(), getY() + 0.5, getZ(),
                        10, // count
                        0.2, 0.2, 0.2, // x, y, z offset
                        0.0 // speed
                );
            }

            // reduce brush durability
            item.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));

            return InteractionResult.sidedSuccess(level().isClientSide);
        } else if (hasPearl() && isOpen()) {
            popPearl();
            entityData.set(OPEN, false);
            entityData.set(HAS_PEARL, false);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }


    private void launchEntity() {
        AABB box = getBoundingBox().move(0, 0.4, 0);
        List<LivingEntity> entities = level().getEntitiesOfClass(LivingEntity.class, box);
        for (LivingEntity entity : entities) {
            if (entity == this) {
                continue;
            }
            if (entity.onGround()) {
                // Get direction clam is facing
                float yaw = this.getYRot(); // Degrees
                double xDir = -Math.sin(Math.toRadians(yaw));
                double zDir = Math.cos(Math.toRadians(yaw));
                Vec3 backward = new Vec3(xDir, 0, zDir).scale(-2);

                Vec3 launchVec = backward.add(0, 2, 0);

                entity.setDeltaMovement(launchVec);
                entity.hurtMarked = true;

                level().playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 4.0f, 1.0f);
            }
        }
    }

    private void popPearl() {
        ItemEntity pearl = new ItemEntity(level(), getX(), getY() + 1, getZ(), new ItemStack(Items.ENDER_PEARL));
        level().addFreshEntity(pearl);
        consumePearl();
    }

    private void consumePearl() {
        pearlRegenTimer = 20 * (120 + random.nextInt(60)); // 2-3 mins
        setHasPearl(false);
    }

    public boolean hasPearl() {
        return this.entityData.get(HAS_PEARL);
    }

    public boolean isOpen() {
        return this.entityData.get(OPEN);
    }

    public void setHasPearl(boolean value) {
        this.entityData.set(HAS_PEARL, value);
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> animationState) {
        AnimationController<?> controller = animationState.getController();

        if (entityData.get(FLING_ANIM)) {
            controller.setAnimation(RawAnimation.begin().then("pushing", Animation.LoopType.PLAY_ONCE));

            if (controller.hasAnimationFinished()) {
                entityData.set(FLING_ANIM, false);
                controller.forceAnimationReset();
            }

            return PlayState.CONTINUE;
        }

        boolean isOpen = entityData.get(OPEN);

        if (isOpen) {
            controller.setAnimation(RawAnimation.begin().then("open", Animation.LoopType.HOLD_ON_LAST_FRAME));
        } else {
            // only play close once when transitioning from open to closed
            if (lastOpenState) {
                controller.setAnimation(RawAnimation.begin().then("close", Animation.LoopType.HOLD_ON_LAST_FRAME));
            }
        }

        lastOpenState = isOpen;
        return PlayState.CONTINUE;
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(FLING_ANIM, false);
        entityData.define(HAS_PEARL, true);
        entityData.define(OPEN, false);
    }
}
