package com.jeremyseq.inhabitants.entities.bogre;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.ModSoundEvents;
import com.jeremyseq.inhabitants.entities.EntityUtil;
import com.jeremyseq.inhabitants.entities.PrecisePathNavigation;
import com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron.BogreCauldronEntity;
import com.jeremyseq.inhabitants.entities.goals.CooldownMeleeAttackGoal;
import com.jeremyseq.inhabitants.items.ModItems;
import com.jeremyseq.inhabitants.networking.ModNetworking;
import com.jeremyseq.inhabitants.networking.ScreenShakePacketS2C;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
import java.util.function.Predicate;

public class BogreEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public enum State {
        CAUTIOUS,
        MAKE_CHOWDER,
        CARVE_BONE
    }

    boolean pathSet = false;

    public State state = State.CAUTIOUS;

    /**
     * On client side, this is used to start the roar animation and is set to false immediately after starting.
     * On server side, this is used to determine if the Bogre is currently roaring and is set to false after ROAR_TICKS
     */
    public static final EntityDataAccessor<Boolean> ROAR_ANIM = SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> COOKING_TICKS = SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.INT); // used to time cooking
    public static final EntityDataAccessor<Boolean> COOKING_ANIM = SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> CARVING_ANIM = SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DANCING = SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int JUKEBOX_RANGE = 15;

    private enum DancePhase {
        NONE,
        START,
        LOOP,
        END
    }

    private DancePhase dancePhase = DancePhase.NONE; // client only

    public static float FORGET_RANGE = 20f;
    public static float ROAR_RANGE = 12f;
    public static float HOSTILE_RANGE = 6f;
    public static final double MAX_CAULDRON_DIST_SQR = 14*14;

    public BlockPos cauldronPos = null;
    public BlockPos entrancePos = null;


    // STUCK FAILSAFE FOR MAKE_CHOWDER
    private Vec3 lastPos = null;
    private int stuckTicks = 0;
    private static final int STUCK_TICK_LIMIT = 40; // 2 seconds
    private static final double MIN_MOVE_SQ = 0.0025; // 0.05 blocks


    private static final int ROAR_TICKS = 45; // how long a roar animation lasts (server)

    private int roaringTick = 0; // used on both client and server separately
    private Player roaredPlayer = null; // the player that the Bogre is currently roaring at
    private final List<Player> warnedPlayers = new ArrayList<>();

    // CARVE_BONE
    private static final int BONE_CARVE_TIME_TICKS = 160; // time it takes to carve a bone
    private static final int BONE_CARVE_DESTROY_BONES_TICKS = 130; // when to destroy the bone blocks
    private int boneCarveTicks = 0; // counts down while carving a bone
    private List<BlockPos> boneBlockPositions; // the positions of the bone blocks to carve

    // MAKE CHOWDER
    public static final EntityDataAccessor<ItemStack> ITEM_HELD = // if the Bogre has picked up the fish item
            SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.ITEM_STACK);

    private ItemEntity droppedIngredientItem = null;
    private Player droppedIngredientPlayer = null; // the player that dropped the fish item
    private static final double INGREDIENT_REACH_DISTANCE = 3;
    private static final int CHOWDER_TIME_TICKS = 160;
    private static final int DROP_FISH_OFFSET = 10; // at what time the Bogre should drop the fish item after starting to make chowder
    private int chowderThrowDelay = -1;

    // list of players who have tamed the Bogre
    private final Set<UUID> tamedPlayers = new HashSet<>();


    // ATTACK
    private static final int ATTACK_DELAY = 19; // ticks before the shockwave is triggered after the attack animation starts
    private int attackAnimTimer = 0;

    private boolean randomChance = false; // used to trigger a rare idle animation

    private static final double SHOCKWAVE_RADIUS = 7;
    private static final float SHOCKWAVE_DAMAGE = 32f; // damage at the center of the shockwave

    public BogreEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setPersistenceRequired();
        ((GroundPathNavigation) this.getNavigation()).setAvoidSun(false);
    }

    public static AttributeSupplier setAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 400.0F)
                .add(Attributes.ATTACK_DAMAGE, 30F)
                .add(Attributes.ATTACK_SPEED, .5)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5F)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0F)
                .add(Attributes.MOVEMENT_SPEED, .17f).build();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(4, new CooldownMeleeAttackGoal(this, 1.3f, false, 40, true, true));
        this.goalSelector.addGoal(5, new BogreReturnToCauldronGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new BogreConditionalStrollGoal(this, 1.0D));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "defaults_controller", 0, this::defaults).transitionLength(3));
        controllerRegistrar.add(new AnimationController<>(this, "hurt", 0, state -> PlayState.STOP)
                .triggerableAnim("hurt", RawAnimation.begin().then("taking_damage", Animation.LoopType.PLAY_ONCE)));
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
        controllerRegistrar.add(new AnimationController<>(this, "grab", 0, state -> PlayState.STOP)
                .triggerableAnim("grab", RawAnimation.begin().then("grab", Animation.LoopType.PLAY_ONCE)));
        controllerRegistrar.add(new AnimationController<>(this, "attack", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE)));
    }


    private <T extends GeoAnimatable> PlayState defaults(AnimationState<T> animationState) {

        if (entityData.get(DANCING) && dancePhase == DancePhase.NONE) {
            this.dancePhase = DancePhase.START;
        }
        if (!entityData.get(DANCING) && dancePhase == DancePhase.LOOP) {
            this.dancePhase = DancePhase.END;
        }

        if (this.dancePhase != DancePhase.NONE) {
            switch (dancePhase) {
                case START -> {
                    animationState.getController().setAnimation(
                            RawAnimation.begin().then("dance start", Animation.LoopType.PLAY_ONCE)
                    );

                    if (animationState.getController().hasAnimationFinished()) {
                        dancePhase = DancePhase.LOOP;
                        animationState.getController().forceAnimationReset();
                    }
                    return PlayState.CONTINUE;
                }

                case LOOP -> {
                    animationState.getController().setAnimation(
                            RawAnimation.begin().then("dance", Animation.LoopType.LOOP)
                    );
                    return PlayState.CONTINUE;
                }

                case END -> {
                    animationState.getController().setAnimation(
                            RawAnimation.begin().then("dance end", Animation.LoopType.PLAY_ONCE)
                    );

                    if (animationState.getController().hasAnimationFinished()) {
                        entityData.set(DANCING, false);
                        dancePhase = DancePhase.NONE;
                        animationState.getController().forceAnimationReset();
                    }
                    return PlayState.CONTINUE;
                }
            }
        }

        if (animationState.isMoving()) {
            if (this.isSprinting()) {
                animationState.getController().setAnimation(RawAnimation.begin().then("run", Animation.LoopType.LOOP));
            } else {
                animationState.getController().setAnimation(RawAnimation.begin().then("walk", Animation.LoopType.LOOP));
            }
        } else {
            if (randomChance) {
                animationState.getController().setAnimation(RawAnimation.begin().then("idle_rare", Animation.LoopType.PLAY_ONCE));
                if (animationState.getController().hasAnimationFinished()) {
                    animationState.getController().forceAnimationReset();
                    randomChance = false;
                    animationState.getController().forceAnimationReset();
                }
                return PlayState.CONTINUE;
            }

            animationState.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.PLAY_ONCE));
            if (animationState.getController().hasAnimationFinished()) {
                animationState.getController().forceAnimationReset();
                randomChance = new Random().nextFloat() < 0.1f; // chance to trigger a rare idle animation
            }
        }
        return PlayState.CONTINUE;
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> animationState) {
        if (isRoaring()) {
            animationState.getController().setAnimation(RawAnimation.begin().then("roar", Animation.LoopType.PLAY_ONCE));
            if (animationState.getController().hasAnimationFinished()) {
                animationState.getController().forceAnimationReset();
            }
            return PlayState.CONTINUE;
        }

        if (entityData.get(COOKING_ANIM)) {
            animationState.getController().setAnimation(RawAnimation.begin().then("cooking", Animation.LoopType.PLAY_ONCE));
            if (animationState.getController().hasAnimationFinished()) {
                entityData.set(COOKING_ANIM, false);
                animationState.getController().forceAnimationReset();
            }
            return PlayState.CONTINUE;
        }

        if (entityData.get(CARVING_ANIM)) {
            animationState.getController().setAnimation(RawAnimation.begin().then("carving", Animation.LoopType.PLAY_ONCE));
            if (animationState.getController().hasAnimationFinished()) {
                entityData.set(CARVING_ANIM, false);
                animationState.getController().forceAnimationReset();
            }
            return PlayState.CONTINUE;
        }
        return PlayState.CONTINUE;
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        if (!level().isClientSide && this.attackAnimTimer == 0) {
            triggerAnim("attack", "attack");
            this.playSound(ModSoundEvents.BOGRE_ATTACK.get(), 1, 1);
            this.attackAnimTimer = ATTACK_DELAY;
        }
        return true;
    }

    @Override
    public void customServerAiStep() {

        if (!isJukeboxPlayingNearby() && entityData.get(DANCING)) {
            // stop dancing
            entityData.set(DANCING, false);
        }

        if (entityData.get(DANCING)) {
            this.getNavigation().stop();
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        if (cauldronPos == null || !isValidCauldron(cauldronPos)) {
            Optional<BlockPos> nearestCauldron = BlockPos.betweenClosedStream(blockPosition().offset(-10, -2, -10), blockPosition().offset(10, 2, 10))
                    .map(BlockPos::immutable)
                    .filter(this::isValidCauldron)
                    .findFirst();

            this.cauldronPos = nearestCauldron.orElse(null);
        }

        if (this.cauldronPos == null) {
            this.tamedPlayers.clear(); // clear tamed players if no cauldron is found
        } else {

            // set entrance pos
            BogreCauldronEntity bogreCauldron = getCauldronEntity();
            if (bogreCauldron == null) {
                this.state = State.CAUTIOUS;
                return;
            }
            Direction direction = bogreCauldron.getDirection();
            Direction dirLeft = direction.getCounterClockWise(Direction.Axis.Y);

            // offsets for where the bogre should stand relative to cauldron (facing forward)
            final float forwardDist = 4;
            final float rightDist = 11;

            Vec3i forwardI = direction.getNormal();
            Vec3i rightI = dirLeft.getNormal();
            Vec3 forward = new Vec3(forwardI.getX(), forwardI.getY(), forwardI.getZ()).scale(forwardDist);
            Vec3 right = new Vec3(rightI.getX(), rightI.getY(), rightI.getZ()).scale(rightDist);

            Vec3 targetCenter = Vec3.atBottomCenterOf(cauldronPos).add(forward).subtract(right);

            this.entrancePos = BlockPos.containing(targetCenter.x, targetCenter.y, targetCenter.z);
        }

        // attack animation timer
        if (this.attackAnimTimer > 0) {
            this.attackAnimTimer--;
            if (this.attackAnimTimer == 0) {
                EntityUtil.shockwave(this, SHOCKWAVE_RADIUS, SHOCKWAVE_DAMAGE, entity -> entity == this, false);
            }
        }

        if (this.getTarget() != null) {
            if (this.distanceTo(this.getTarget()) > FORGET_RANGE || !this.getTarget().isAlive() || this.getTarget().isDeadOrDying()) {
                this.setTarget(null);
            } else if (this.getTarget() instanceof Player player && (player.isCreative() || player.isSpectator())) {
                this.setTarget(null);
            } else {
                // if the target is still valid, do not do custom AI step
                return;
            }
        }

        if (this.state == State.CAUTIOUS) {
            if (isJukeboxPlayingNearby() && !entityData.get(DANCING)) {
                // start dancing
                entityData.set(DANCING, true);

                this.playSound(SoundEvents.WARDEN_LISTENING, 1, 1);

                this.getNavigation().stop();
                this.setTarget(null);
                return;
            }

            cautiousAiStep();
        } else if (this.state == State.MAKE_CHOWDER) {
            makeChowderAiStep();
        } else if (this.state == State.CARVE_BONE) {
            carveBoneAiStep();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.isRoaring()) {
                if (roaringTick == 10 || roaringTick == 15) {
                    EntityUtil.screamParticles((ClientLevel) this.level(), new Vec3(getX(), getY() + 0.5, getZ()), this.getLookAngle(), .5f);
                }

                roaringTick++;
            } else {
                roaringTick = 0;
            }
        }
    }

    public int getRoaringTick() {
        return roaringTick;
    }

    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        boolean result = super.hurt(pSource, pAmount);
        if (result && !level().isClientSide) {
            this.triggerAnim("hurt", "hurt");
        }
        // if attacked by a player, switch to cautious state and set the player as target
        entityData.set(DANCING, false);
        this.state = State.CAUTIOUS;
        if (pSource.getEntity() instanceof Player player && player.isAlive() && !player.isCreative()) {
            if (!this.getItemHeld().isEmpty()) {
                this.throwHeldItem();
            }
            // if the attacker is a player, remove them from tamedPlayers
            tamedPlayers.remove(player.getUUID());
            this.setTarget(player);
        }
        return result;
    }

    /**
     * The Bogre carves a giant bone from 3 bone blocks.
     * This is a placeholder method for future implementation.
     */
    private void carveBoneAiStep() {
        if (boneBlockPositions == null || boneBlockPositions.size() < 3) {
            this.state = State.CAUTIOUS; // revert to cautious state if not enough bone blocks found
            return;
        }

        // ensure the bone blocks are still valid, if they haven't been removed
        if (this.boneCarveTicks < BONE_CARVE_DESTROY_BONES_TICKS) {
            for (BlockPos pos : boneBlockPositions) {
                BlockState state = this.level().getBlockState(pos);
                if (!state.is(Blocks.BONE_BLOCK)) {
                    Inhabitants.LOGGER.debug("Bogre found invalid bone block at: {}", pos);
                    this.state = State.CAUTIOUS; // revert to cautious state if any block is not a bone block
                    return;
                }
            }
        }

        // move to the carving site
        BlockPos center = getAveragePosition(boneBlockPositions);
        double distance = this.distanceToSqr(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        distance = Math.sqrt(distance);
        if (distance > 2.5) {
            this.moveTo(center, 1);
            return;
        }

        // carving
        this.getNavigation().stop();
        this.lookAt(EntityAnchorArgument.Anchor.FEET, Vec3.atCenterOf(center));
        this.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(center));
        if (boneCarveTicks == 0) {
            entityData.set(CARVING_ANIM, false);
            entityData.set(CARVING_ANIM, true);
        }
        boneCarveTicks++;
        if (boneCarveTicks >= BONE_CARVE_TIME_TICKS + 40) {
            this.state = State.CAUTIOUS;
            boneCarveTicks = 0;
        } else if (boneCarveTicks == BONE_CARVE_TIME_TICKS) {
            Inhabitants.LOGGER.debug("BONE CARVING COMPLETE!");
            this.triggerAnim("grab", "grab");
            EntityUtil.throwItemStack(this.level(), this, new ItemStack(ModItems.GIANT_BONE.get()), .3f, 0.3f);
        } else if (boneCarveTicks == BONE_CARVE_DESTROY_BONES_TICKS) {
            // remove the bone blocks
            for (BlockPos pos : boneBlockPositions) {
                if (this.level().getBlockState(pos).is(Blocks.BONE_BLOCK)) {
                    this.level().destroyBlock(pos, false); // remove block without drops
                }
            }
            this.playSound(SoundEvents.STONE_BREAK, 1.0F, 0.7F); // some kind of cracking/carving sound
        }
    }

    private boolean isJukeboxPlayingNearby() {
        BlockPos origin = this.blockPosition();

        return BlockPos.betweenClosedStream(
                origin.offset(-JUKEBOX_RANGE, -2, -JUKEBOX_RANGE),
                origin.offset(JUKEBOX_RANGE, 2, JUKEBOX_RANGE)
        ).anyMatch(pos -> {
            BlockState state = level().getBlockState(pos);
            return state.is(Blocks.JUKEBOX)
                    && state.hasProperty(JukeboxBlock.HAS_RECORD)
                    && state.getValue(JukeboxBlock.HAS_RECORD);
        });
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level pLevel) {
        return new PrecisePathNavigation(this, pLevel);
    }

    private void throwHeldItem() {
        assert !this.level().isClientSide();
        this.triggerAnim("grab", "grab");
        EntityUtil.throwItemStack(this.level(), this, this.getItemHeld(), .3f, 0);
        setItemHeld(ItemStack.EMPTY);
    }

    /**
     * The Bogre attempts to make chowder from a fish dropped by a player.
     * The fish should be droppedFishItem.
     */
    private void makeChowderAiStep() {
        if (!this.getItemHeld().isEmpty() && isHoldingChowder()) {
            // HOLDING CHOWDER

            // chowder is dropped after a short delay during which the bogre turns to the player
            if (droppedIngredientPlayer != null) {
                if (chowderThrowDelay == -1) {
                    this.lookAt(EntityAnchorArgument.Anchor.FEET, droppedIngredientPlayer.position());
                    chowderThrowDelay = 20; // short delay before throwing chowder
                    return;
                } else if (chowderThrowDelay > 0) {
                    // wait
                    chowderThrowDelay--;
                    return;
                } else if (chowderThrowDelay == 0) {
                    throwHeldItem();
                    this.state = State.CAUTIOUS;
                    droppedIngredientPlayer = null;
                    chowderThrowDelay = -1;
                    return;
                }
            } else {
                // fallback if no player
                throwHeldItem();
                this.state = State.CAUTIOUS;
                chowderThrowDelay = -1;
                return;
            }
        } else if (!this.getItemHeld().isEmpty()) {
            // HOLDING INGREDIENT

            // find the target block, which is 3 blocks in front of the cauldron in the direction it is facing
            BogreCauldronEntity bogreCauldron = getCauldronEntity();
            if (bogreCauldron == null) {
                this.state = State.CAUTIOUS;
                return;
            }
            Direction direction = bogreCauldron.getDirection();
            Direction dirLeft = direction.getCounterClockWise(Direction.Axis.Y);

            // offsets for where the bogre should stand relative to cauldron (facing forward)
            final float forwardDist = 2.25f;
            final float leftDist = .9f;

            Vec3i forwardI = direction.getNormal();
            Vec3i leftI = dirLeft.getNormal();
            Vec3 forward = new Vec3(forwardI.getX(), forwardI.getY(), forwardI.getZ()).scale(forwardDist);
            Vec3 left = new Vec3(leftI.getX(), leftI.getY(), leftI.getZ()).scale(leftDist);

            Vec3 targetCenter = Vec3.atBottomCenterOf(cauldronPos).add(forward).add(left);

            double distSqr = this.distanceToSqr(targetCenter);

            PrecisePathNavigation preciseNav = (PrecisePathNavigation) this.getNavigation();

            Vec3 currentPos = this.position();

            if (lastPos != null) {
                double movedSq = currentPos.distanceToSqr(lastPos);

                if (movedSq < MIN_MOVE_SQ && distSqr > 0.3) {
                    stuckTicks++;
                } else {
                    stuckTicks = 0;
                }
            }

            lastPos = currentPos;

            // FAILSAFE: teleport if stuck
            if (stuckTicks > STUCK_TICK_LIMIT && this.entrancePos != null) {
                Vec3 tp = Vec3.atCenterOf(this.entrancePos);

                this.setPos(tp.x, tp.y, tp.z);
                this.getNavigation().stop();

                stuckTicks = 0;
                pathSet = false;

                return;
            }

            if (!pathSet && distSqr > .3) {
                // starting path to cauldron
                preciseNav.preciseMoveTo(targetCenter, 1.0D);
                pathSet = true;
                return;
            }

            if (distSqr > .3) {
                // walking to cauldron
                return;
            }

            // arrived at cauldron
            pathSet = false;
            this.getNavigation().stop();

            this.lookAt(EntityAnchorArgument.Anchor.FEET, cauldronPos.getCenter());
            this.lookAt(EntityAnchorArgument.Anchor.EYES, cauldronPos.getCenter());
            // start cooking animation
            if (getCookingTicks() == DROP_FISH_OFFSET) {
                triggerAnim("grab", "grab");
            } else if (getCookingTicks() == 25) {
                entityData.set(COOKING_ANIM, false);
                entityData.set(COOKING_ANIM, true);
                this.playSound(SoundEvents.WARDEN_DIG, 1.0F, 1f);
            }

            setCookingTicks(getCookingTicks()+1);
            if (getCookingTicks() >= CHOWDER_TIME_TICKS) {
                Inhabitants.LOGGER.debug("CHOWDER COMPLETE!");

                // chowder complete
                this.playSound(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 1.0F, 0.8F); // play something watery?
                tamedPlayers.add(droppedIngredientPlayer.getUUID()); // add the player that dropped the ingredient to the tamed list

                // 30% chance of suspicious stew with random effects
                if (this.level().random.nextFloat() < 0.3f) {
                    ItemStack stew = new ItemStack(Items.SUSPICIOUS_STEW);
                    MobEffectInstance[] effects = new MobEffectInstance[] {
                            new MobEffectInstance(MobEffects.SATURATION, 7),
                            new MobEffectInstance(MobEffects.NIGHT_VISION, 80),
                            new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 80),
                            new MobEffectInstance(MobEffects.BLINDNESS, 160),
                            new MobEffectInstance(MobEffects.WEAKNESS, 180),
                            new MobEffectInstance(MobEffects.REGENERATION, 160),
                            new MobEffectInstance(MobEffects.JUMP, 80),
                            new MobEffectInstance(MobEffects.POISON, 240),
                            new MobEffectInstance(MobEffects.WITHER, 160)
                    };

                    MobEffectInstance chosen =
                            effects[this.level().random.nextInt(effects.length)];
                    SuspiciousStewItem.saveMobEffect(stew, chosen.getEffect(), chosen.getDuration());

                    this.setItemHeld(stew);
                } else {
                    if (this.getItemHeld().is(Items.POISONOUS_POTATO)) {
                        this.setItemHeld(new ItemStack(ModItems.STINKY_BOUILLON.get()));
                    } else if (this.getItemHeld().is(Items.ROTTEN_FLESH) ||  this.getItemHeld().is(Items.SPIDER_EYE)) {
                        this.setItemHeld(new ItemStack(ModItems.UNCANNY_POTTAGE.get()));
                    } else {
                        this.setItemHeld(new ItemStack(ModItems.FISH_SNOT_CHOWDER.get()));
                    }
                }

                setCookingTicks(0);
            }

            return;
        }

        // DID NOT YET PICK UP INGREDIENT

        if (droppedIngredientItem == null || !droppedIngredientItem.isAlive()) {
            this.state = State.CAUTIOUS;
            return;
        }

        double distance = this.distanceTo(droppedIngredientItem);
        if (distance > INGREDIENT_REACH_DISTANCE) {
            BlockPos pos = new BlockPos(droppedIngredientItem.getBlockX(), droppedIngredientItem.getBlockY(), droppedIngredientItem.getBlockZ());
            this.moveTo(pos, 1, false);
            return;
        }

        if (this.getItemHeld().isEmpty()) {
            this.triggerAnim("grab", "grab");
            this.getNavigation().stop();
            ItemStack ingredientStack = droppedIngredientItem.getItem();
            Item ingredientItem = ingredientStack.getItem();

            if (ingredientStack.getCount() > 1) {
                ingredientStack.shrink(1); // remove only one ingredient
            } else {
                droppedIngredientItem.discard(); // discard the item if it was the last one
            }

            this.setItemHeld(new ItemStack(ingredientItem, 1)); // set the holding ingredient state
            droppedIngredientItem = null; // reset the dropped ingredient item
            return;
        }
    }

    private BogreCauldronEntity getCauldronEntity() {
        if (this.cauldronPos == null) return null;

        // find the cauldron entity at the cauldronPos
        List<BogreCauldronEntity> entities = this.level().getEntitiesOfClass(
                BogreCauldronEntity.class,
                new AABB(cauldronPos),
                entity -> !entity.isRemoved()
        );
        if (entities.isEmpty()) {
            return null;
        }
        return entities.get(0);
    }

    private void cautiousAiStep() {
        if (this.getTarget() != null) {
            if (this.distanceTo(this.getTarget()) > FORGET_RANGE) {
                this.setTarget(null);
            }
        }

        if (this.getTarget() == null) {
            List<Player> withinHostileRange = this.level().getEntitiesOfClass(Player.class,
                    getBoundingBox().inflate(HOSTILE_RANGE), Predicate.not(Player::isSpectator));

            // sort players by distance to the Bogre, closest to farthest
            withinHostileRange.sort((p1, p2) -> Float.compare(p1.distanceTo(this), p2.distanceTo(this)));
            for (Player player : withinHostileRange) {
                if (!this.isTamedBy(player) && player.distanceTo(this) <= HOSTILE_RANGE && this.hasLineOfSight(player)
                        && !player.isCreative() && !player.isSpectator()) {
                    this.setTarget(player);
                    break;
                }
            }
        }

        // search for players within ROAR_RANGE
        if (!isRoaring()) {
            List<Player> withinRoarRange = this.level().getEntitiesOfClass(Player.class,
                    getBoundingBox().inflate(ROAR_RANGE), Predicate.not(Player::isSpectator));

            // sort players by distance to the Bogre, closest to farthest
            withinRoarRange.sort((p1, p2) -> Float.compare(p1.distanceTo(this), p2.distanceTo(this)));
            for (Player player : withinRoarRange) {
                if (!this.isTamedBy(player) && !warnedPlayers.contains(player)
                        && player.distanceTo(this) <= ROAR_RANGE && this.hasLineOfSight(player)
                        && !player.isCreative() && !player.isSpectator()) {
                    // face player and start roaring
                    roaredPlayer = player;
                    this.lookControl.setLookAt(roaredPlayer);
                    setRoaring(true);
                    warnedPlayers.add(player);

                    // play roar sound
                    this.level().playSound(null, this.blockPosition(), ModSoundEvents.BOGRE_ROAR.get(),
                            SoundSource.HOSTILE, 2.0F, 0.9F + this.level().random.nextFloat() * 0.2F);

                    break;
                }
            }
        } else {
            this.lookAt(EntityAnchorArgument.Anchor.EYES, roaredPlayer.position());
            roaringTick++;
            if (roaringTick >= ROAR_TICKS) {
                setRoaring(false);
                roaredPlayer = null; // reset the roared player
                roaringTick = 0;
            }
            if (roaringTick == 10) {
                // screen shake effect for the player
                ModNetworking.sendToPlayer(new ScreenShakePacketS2C(80), (ServerPlayer) roaredPlayer);
            }
        }

        // prune warnedPlayers list
        warnedPlayers.removeIf(player -> player.distanceTo(this) > FORGET_RANGE);


        // if roaring, keep looking at the roared player
        if (isRoaring() && roaredPlayer != null) {
            this.lookControl.setLookAt(roaredPlayer, 30.0F, 30.0F);
        } else {
            // otherwise look at the nearest player within FORGET_RANGE
            List<Player> nearbyPlayers = this.level().getEntitiesOfClass(Player.class,
                    getBoundingBox().inflate(FORGET_RANGE), (player -> !player.isSpectator() && !player.isCreative()));

            if (!nearbyPlayers.isEmpty()) {
                nearbyPlayers.sort((a, b) -> Float.compare(a.distanceTo(this), b.distanceTo(this)));
                Player nearest = nearbyPlayers.get(0);

                if (this.hasLineOfSight(nearest)) {
                    this.lookControl.setLookAt(nearest, 30.0F, 30.0F);
                }
            }
        }

        if (this.cauldronPos == null || !this.isValidCauldron(this.cauldronPos)) {
            // if no cauldron is assigned, don't attempt to make chowder or carve bone
            return;
        }

        // detect fish dropped by non-hostile players
        List<Player> possibleFishDroppers = this.level().getEntitiesOfClass(Player.class,
                getBoundingBox().inflate(FORGET_RANGE), Predicate.not(Player::isSpectator));

        for (Player player : possibleFishDroppers) {
            float distance = player.distanceTo(this);
            if (distance <= FORGET_RANGE) {
                if (distance > HOSTILE_RANGE || this.isTamedBy(player) || player.isCreative()) {
                    // get nearby item entities (fish on ground)
                    List<ItemEntity> nearbyItems = this.level().getEntitiesOfClass(ItemEntity.class,
                            player.getBoundingBox().inflate(4), // check small radius around player
                            item -> item.isAlive() &&
                                    (item.getItem().is(Items.COD)
                                            || item.getItem().is(Items.SALMON)
                                            || item.getItem().is(Items.TROPICAL_FISH)
                                            || item.getItem().is(Items.PUFFERFISH)
                                            || item.getItem().is(Items.ROTTEN_FLESH)
                                            || item.getItem().is(Items.SPIDER_EYE)
                                            || item.getItem().is(Items.POISONOUS_POTATO)
                                    )
                    );

                    for (ItemEntity ingredient : nearbyItems) {
                        if (this.hasLineOfSight(ingredient)) {

                            ingredient.setExtendedLifetime();
                            droppedIngredientItem = ingredient;
                            droppedIngredientPlayer = player; // store the player that dropped the fish
                            this.state = State.MAKE_CHOWDER; // change state to make chowder
                            return;
                        }
                    }
                }
            }
        }

        // check for bone blocks to carve
        List<BlockPos> boneBlockPositions = findThreeBoneBlocksInLine((int) ROAR_RANGE);
        if (boneBlockPositions != null && boneBlockPositions.size() >= 3) {
            if (findNearbyTrustedPlayer(getAveragePosition(boneBlockPositions), 5) != null) {
                // if a trusted player is nearby, carve the bone blocks
                Inhabitants.LOGGER.debug("Found bone blocks to carve: {}", boneBlockPositions);
                this.boneBlockPositions = boneBlockPositions; // store the positions for carving
                this.state = State.CARVE_BONE; // change state to carve bone
                return;
            }
        }
    }

    /**
     * ray trace from bogre's eyes to the cauldron center
     * returns true if no blocks obstruct the view
     */
    public boolean canSeeCauldron() {
        Vec3 eyePos = this.getEyePosition();
        Vec3 target = Vec3.atCenterOf(this.cauldronPos);

        HitResult hit = this.level().clip(new ClipContext(
                eyePos,
                target,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));

        if (hit.getType() == HitResult.Type.MISS) {
            return true;
        }

        if (hit instanceof BlockHitResult bhr) {
            // allow line of sight if the first block hit is the cauldron itself
            return bhr.getBlockPos().equals(this.cauldronPos);
        }

        return false;
    }

    private boolean moveTo(BlockPos pos, double speed, boolean checkCauldronDistance) {
        if (checkCauldronDistance && this.cauldronPos != null && this.cauldronPos.distToCenterSqr(pos.getX(), pos.getY(), pos.getZ()) > MAX_CAULDRON_DIST_SQR) {
            return false;
        }
        this.getNavigation().moveTo(this.getNavigation().createPath(pos, 0), speed);
        return true;
    }

    private boolean moveTo(BlockPos pos, double speed) {
        return moveTo(pos, speed, true);
    }

    public boolean isRoaring() {
        return entityData.get(ROAR_ANIM);
    }

    private void setRoaring(boolean roaring) {
        entityData.set(ROAR_ANIM, roaring);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSoundEvents.BOGRE_IDLE.get();
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) {
        return ModSoundEvents.BOGRE_HURT.get();
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return ModSoundEvents.BOGRE_DEATH.get();
    }

    protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        this.playSound(pState.getSoundType().getStepSound(), this.isSprinting() ? 2f : 1.25f, 0.9f);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ROAR_ANIM, false);
        entityData.define(COOKING_ANIM, false);
        entityData.define(COOKING_TICKS, 0);
        entityData.define(CARVING_ANIM, false);
        entityData.define(ITEM_HELD, ItemStack.EMPTY);
        entityData.define(DANCING, false);
    }

    public boolean isTamedBy(Player player) {
        return tamedPlayers.contains(player.getUUID());
    }

    private boolean isValidCauldron(BlockPos pos) {
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY();
        double centerZ = pos.getZ() + 0.5;

        // Search for BogreCauldronEntities within a small radius around the center
        List<BogreCauldronEntity> entities = level().getEntitiesOfClass(
                BogreCauldronEntity.class,
                new AABB(centerX - 0.5, centerY - 1, centerZ - 0.5, centerX + 0.5, centerY + 2, centerZ + 0.5)
        );

        // Check if any of those entities are centered at the given block position
        for (BogreCauldronEntity entity : entities) {
            BlockPos entityCenter = entity.blockPosition();
            if (entityCenter.equals(pos)) {
                return true;
            }
        }

        return false;
    }


    public ItemStack getItemHeld() {
        return this.entityData.get(ITEM_HELD);
    }

    /**
     * Used specifically by the client to determine what to render in the Bogre's hand.
     */
    public ItemStack getAnimateItemHeld() {
        if (!this.getItemHeld().isEmpty() && !isHoldingChowder()) {
            // if the chowder animation is at the drop fish offset, return an empty stack
            if (this.getCookingTicks() >= DROP_FISH_OFFSET+2) {
                return ItemStack.EMPTY;
            }
        }
        return this.getItemHeld();
    }

    private boolean isHoldingChowder() {
        return this.getItemHeld().is(ModItems.FISH_SNOT_CHOWDER.get())
                || this.getItemHeld().is(ModItems.UNCANNY_POTTAGE.get())
                || this.getItemHeld().is(ModItems.STINKY_BOUILLON.get())
                || this.getItemHeld().is(Items.SUSPICIOUS_STEW);
    }

    private void setItemHeld(ItemStack itemHeld) {
        // play pickup sound
        this.playSound(SoundEvents.ITEM_PICKUP, 1, 1);
        this.entityData.set(ITEM_HELD, itemHeld);
    }

    @Override
    public float getStepHeight() {
        return 1.5f;
    }

    @Override
    public boolean onClimbable() {
        return false; // not allowed to climb (vines or ladders)
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.BRACER_OF_MIGHT.get()));
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        ListTag list = new ListTag();
        for (UUID id : tamedPlayers) {
            CompoundTag idTag = new CompoundTag();
            idTag.putUUID("uuid", id);
            list.add(idTag);
        }
        tag.put("tamedPlayers", list);

        if (cauldronPos != null) {
            tag.put("CauldronPos", NbtUtils.writeBlockPos(cauldronPos));
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        tamedPlayers.clear();

        if (tag.contains("tamedPlayers", Tag.TAG_LIST)) {
            ListTag list = tag.getList("tamedPlayers", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag idTag = list.getCompound(i);
                UUID id = idTag.getUUID("uuid");
                tamedPlayers.add(id);
            }
        }

        if (tag.contains("CauldronPos")) {
            cauldronPos = NbtUtils.readBlockPos(tag.getCompound("CauldronPos"));
        }
    }

    private @Nullable Player findNearbyTrustedPlayer(BlockPos center, double radius) {
        List<Player> players = this.level().getEntitiesOfClass(Player.class, new AABB(center).inflate(radius));
        for (Player player : players) {
            if (tamedPlayers.contains(player.getUUID())) {
                return player;
            }
        }
        return null;
    }

    @Nullable
    private List<BlockPos> findThreeBoneBlocksInLine(int radius) {
        BlockPos origin = this.blockPosition();
        List<BlockPos> boneBlocks = new ArrayList<>();

        // collect all bone blocks in the search radius
        BlockPos.betweenClosedStream(origin.offset(-radius, -3, -radius), origin.offset(radius, 3, radius))
                .forEach(pos -> {
                    if (this.level().getBlockState(pos).is(Blocks.BONE_BLOCK)) {
                        boneBlocks.add(pos.immutable());
                    }
                });

        // sort by distance to Bogre
        boneBlocks.sort(Comparator.comparingDouble(pos -> pos.distSqr(origin)));

        Set<BlockPos> boneBlockSet = new HashSet<>(boneBlocks);

        // try to find a line of 3 in any direction centered on one of the blocks
        for (BlockPos pos : boneBlocks) {
            if (boneBlockSet.contains(pos.offset(-1, 0, 0)) && boneBlockSet.contains(pos.offset(1, 0, 0))) {
                return List.of(pos.offset(-1, 0, 0), pos, pos.offset(1, 0, 0));
            }
            if (boneBlockSet.contains(pos.offset(0, 0, -1)) && boneBlockSet.contains(pos.offset(0, 0, 1))) {
                return List.of(pos.offset(0, 0, -1), pos, pos.offset(0, 0, 1));
            }
        }

        return null;
    }

    private BlockPos getAveragePosition(List<BlockPos> positions) {
        int x = 0, y = 0, z = 0;
        for (BlockPos pos : positions) {
            x += pos.getX();
            y += pos.getY();
            z += pos.getZ();
        }
        return new BlockPos(x / positions.size(), y / positions.size(), z / positions.size());
    }

    private int getCookingTicks() {
        return entityData.get(COOKING_TICKS);
    }

    private void setCookingTicks(int ticks) {
        entityData.set(COOKING_TICKS, ticks);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
