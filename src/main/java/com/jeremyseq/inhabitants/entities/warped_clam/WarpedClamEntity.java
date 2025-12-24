package com.jeremyseq.inhabitants.entities.warped_clam;

import com.jeremyseq.inhabitants.ModParticles;
import com.jeremyseq.inhabitants.ModSoundEvents;
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

    public static final EntityDataAccessor<Integer> DIRECTION = SynchedEntityData.defineId(WarpedClamEntity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Boolean> OPEN = SynchedEntityData.defineId(WarpedClamEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> FLING_ANIM = SynchedEntityData.defineId(WarpedClamEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> HAS_PEARL = SynchedEntityData.defineId(WarpedClamEntity.class, EntityDataSerializers.BOOLEAN);

    private int pearlRegenTimer = 0; // Ticks until pearl regenerates
    private int launchCooldown = 0; // ticks until next launch can happen
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

        float yaw = this.getYRot();
        int direction = Math.round(yaw / 45f) & 7;
        setDir(direction);

        return pSpawnData;
    }

    @Override
    protected void playHurtSound(@NotNull DamageSource pSource) {
        level().playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSoundEvents.WARPED_CLAM_OPENED_DAMAGE.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
    }

    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        if (!this.isOpen() && pSource.getEntity() instanceof LivingEntity) {
            level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSoundEvents.WARPED_CLAM_CLOSED_DAMAGE.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
            return false;
        }
        this.closeClam();
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

    public void updateRot() {
        this.setYRot(directionToYaw(getDir()));
        this.setYHeadRot(getYRot());
    }

    @Override
    public void tick() {
        super.tick();

        updateRot();

        // regen pearl
        if (!hasPearl() && pearlRegenTimer > 0) {
            pearlRegenTimer--;
            if (pearlRegenTimer <= 0) {
                setHasPearl(true);
            }
        }

        // Check for players standing on top and trigger launch delay
        if (!level().isClientSide && launchDelayTicks == 0 && launchCooldown <= 0 && !isOpen()) {
            AABB topBox = getBoundingBox().move(0, 0.4, 0); // slightly above the clam
            List<LivingEntity> entities = level().getEntitiesOfClass(LivingEntity.class, topBox);
            entities.remove(this);

            if (!entities.isEmpty()) {
                entityData.set(FLING_ANIM, false);
                entityData.set(FLING_ANIM, true);
                launchDelayTicks = 8;
                launchCooldown = 30;
            }
        }

        // Handle launch delay
        if (launchDelayTicks > 0) {
            launchDelayTicks--;
            if (launchDelayTicks == 0) {
                launchEntity();
            }
        }

        if (launchCooldown < 0) {
            launchCooldown = 0;
        } else if (launchCooldown > 0) {
            launchCooldown--;
        }

        // handle pearl pop delay
        if (popDelayTicks > 0) {
            popDelayTicks--;
            if (popDelayTicks == 0) {
                this.closeClam();
            }
        }

        // particles if clam has pearl and is open
        if (hasPearl() && level().isClientSide && tickCount % 80 == 0 && isOpen()) {
            spawnPearlAmbientParticle();
        }

        // particles if clam has pearl and is closed
        if (hasPearl() && level().isClientSide && tickCount % 10 == 0 && !isOpen()) {
            spawnFloatingIndicatorParticle();
        }

    }

    private void spawnPearlAmbientParticle() {
        // approximate pearl center inside the clam
        Vec3 center = new Vec3(getX(), getY() + getBbHeight()/2, getZ());

        // local axes: forward from look angle, up world-up, right = forward x up
        Vec3 forward = getLookAngle().normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = forward.cross(up).normalize();

        // corner offset: tweak these values to move the particle to the exact corner you want
        double forwardOffset = 0.2;
        double upOffset = 0.3;
        double rightOffset = -0.15;

        Vec3 corner = center
                .add(forward.scale(forwardOffset))
                .add(right.scale(rightOffset))
                .add(up.scale(upOffset));

        level().addParticle(ModParticles.WARPED_CLAM_PEARL_AMBIENCE.get(),
                corner.x, corner.y, corner.z, 0, 0, 0);
    }

    private void spawnFloatingIndicatorParticle() {
        double cx = getX() + (random.nextDouble() - 0.5) * 1.2;
        double cy = getY() + getBbHeight() + (random.nextDouble() * 0.3);
        double cz = getZ() + (random.nextDouble() - 0.5) * 1.2;
        level().addParticle(ModParticles.WARPED_CLAM_PEARL_INDICATOR.get(), cx, cy, cz,
                (random.nextDouble() - 0.5) * 0.01, (random.nextDouble() - 0.1) * 0.02, (random.nextDouble() - 0.5) * 0.01);
    }

    private static float directionToYaw(int dir) {
        return dir * 45f;
    }

    @Override
    protected @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        ItemStack item = player.getItemInHand(hand);

        if (item.getItem() instanceof ShovelItem) {

            if (player.isCrouching()) {
                rotateClam();
                updateRot();
                return InteractionResult.sidedSuccess(level().isClientSide);
            }

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
                popDelayTicks = 20*15; // how long clam stays open
                // play the brushing sound
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.BRUSH_SAND, SoundSource.PLAYERS, 1.0f, 1.0f);

                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        ModSoundEvents.WARPED_CLAM_OPENING.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);

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
            this.closeClam();
            entityData.set(HAS_PEARL, false);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    private void closeClam() {
        if (!level().isClientSide) {
            entityData.set(OPEN, false);
            level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSoundEvents.WARPED_CLAM_CLOSING.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
        }
    }

    private void rotateClam() {
        int dir = (getDir() + 1) & 7;
        setDir(dir);

        level().playSound(null, blockPosition(),
                SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.6f, 1.2f);
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
        controllers.add(new AnimationController<>(this, "open", 0, state -> PlayState.STOP)
                .triggerableAnim("open", RawAnimation.begin().then("Opening", Animation.LoopType.PLAY_ONCE)));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> animationState) {
        AnimationController<?> controller = animationState.getController();

        if (entityData.get(FLING_ANIM)) {
            controller.setAnimation(RawAnimation.begin().then("Pushing", Animation.LoopType.PLAY_ONCE));

            if (controller.hasAnimationFinished()) {
                entityData.set(FLING_ANIM, false);
                controller.forceAnimationReset();
            }

            return PlayState.CONTINUE;
        }

        boolean isOpen = entityData.get(OPEN);

        if (isOpen) {
            controller.setAnimation(RawAnimation.begin().then("Opened", Animation.LoopType.LOOP));
        } else {
            // only play close once when transitioning from open to closed
            if (lastOpenState) {
                controller.setAnimation(RawAnimation.begin().then("closing", Animation.LoopType.HOLD_ON_LAST_FRAME));
            }
        }

        lastOpenState = isOpen;
        return PlayState.CONTINUE;
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> pKey) {
        // trigger open animation when the OPEN state changes
        if (pKey.equals(OPEN) && this.isOpen()) {
            this.triggerAnim("open", "open");
        }
        super.onSyncedDataUpdated(pKey);
    }

    public int getDir() {
        return entityData.get(DIRECTION);
    }
    public void setDir(int dir) {
        entityData.set(DIRECTION, dir & 7);
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
        entityData.define(DIRECTION, 0);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putBoolean("hasPearl", hasPearl());
        tag.putInt("pearlRegenTimer", this.pearlRegenTimer);
        tag.putInt("direction", getDir());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("hasPearl")) {
            this.setHasPearl(tag.getBoolean("hasPearl"));
        }

        if (tag.contains("pearlRegenTimer")) {
            this.pearlRegenTimer = tag.getInt("pearlRegenTimer");
        } else {
            this.pearlRegenTimer = 0;
        }

        if (tag.contains("direction")) {
            setDir(tag.getInt("direction"));
        }
    }
}
