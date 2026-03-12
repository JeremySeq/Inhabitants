package com.jeremyseq.inhabitants.entities.bogre;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.ModSoundEvents;
import com.jeremyseq.inhabitants.entities.EntityUtil;
import com.jeremyseq.inhabitants.entities.PrecisePathNavigation;
import com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron.BogreCauldronEntity;
import com.jeremyseq.inhabitants.entities.bogre.recipe.BogreRecipe;
import com.jeremyseq.inhabitants.entities.bogre.recipe.BogreRecipeManager;
import com.jeremyseq.inhabitants.entities.bogre.recipe.BogreCraftingManager;
import com.jeremyseq.inhabitants.entities.goals.AnimatedCooldownMeleeAttackGoal;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import net.minecraftforge.registries.ForgeRegistries;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.ClientUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class BogreEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public enum State {
        CAUTIOUS,
        MAKE_CHOWDER,
        CARVE_BONE,
        MAKE_DISC
    }

    boolean pathSet = false;

    /**
     * On client side, this is used to start the roar animation and is set to false immediately after starting.
     * On server side, this is used to determine if the Bogre is currently roaring and is set to false after ROAR_TICKS
     */
    public static final EntityDataAccessor<Boolean> ROAR_ANIM = SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> COOKING_TICKS = SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.INT); // used to time cooking
    public static final EntityDataAccessor<Boolean> COOKING_ANIM = SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> CARVING_ANIM = SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DANCING = SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Integer> AI_STATE = SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> HAMMER_SOUND = SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> CARVE_DURATION = SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.INT);

    private static final int JUKEBOX_RANGE = 15;

    private enum DancePhase {
        NONE,
        START,
        LOOP,
        END
    }

    private DancePhase dancePhase = DancePhase.NONE; // client only

    public static final float FORGET_RANGE = 20f;
    public static final float ROAR_RANGE = 12f;
    public static final float HOSTILE_RANGE = 10f;
    public static final double MAX_CAULDRON_DIST_SQR = 14*14;

    public static final int DROP_FISH_OFFSET = 10;
    private static final float INGREDIENT_REACH_DISTANCE = 3.0f;
    private static final int STUCK_TICK_LIMIT = 40;
    private static final double MIN_MOVE_SQ = 0.0025;

    public BlockPos cauldronPos = null;
    public BlockPos entrancePos = null;


    // STUCK FAILSAFE FOR MAKE_CHOWDER
    private Vec3 lastPos = null;
    private int stuckTicks = 0;

    private static final int ROAR_TICKS = 45; // how long a roar animation lasts (server)

    private int roaringTick = 0; // used on both client and server separately
    private Player roaredPlayer = null; // the player that the Bogre is currently roaring at
    private final List<Player> warnedPlayers = new ArrayList<>();

    private BogreRecipe activeRecipe = null;
    private int carveTicks = 0; // counts down while carving a bone
    private List<BlockPos> carvePositions; // the positions of the bone blocks to carve

    // MAKE CHOWDER
    public static final EntityDataAccessor<ItemStack> ITEM_HELD = // if the Bogre has picked up the fish item
            SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.ITEM_STACK);

    private ItemEntity droppedIngredientItem = null;
    private Player droppedIngredientPlayer = null; // the player that dropped the fish item
    private static final int CHOWDER_TIME_TICKS = 160;
    private int chowderThrowDelay = -1;

    // list of players who have tamed the Bogre
    private final Set<UUID> tamedPlayers = new HashSet<>();

    private boolean randomChance = false; // used to trigger a rare idle animation

    private static final float SHOCKWAVE_RADIUS = 9;
    private static final float SHOCKWAVE_DAMAGE = 28f; // damage at the center of the shockwave

    public BogreEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setPersistenceRequired();
        ((GroundPathNavigation) this.getNavigation()).setAvoidSun(false);
    }

    public static AttributeSupplier setAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 300.0F)
                .add(Attributes.ATTACK_DAMAGE, 30F)
                .add(Attributes.ATTACK_SPEED, .5)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5F)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0F)
                .add(Attributes.MOVEMENT_SPEED, .17f).build();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(4, new AnimatedCooldownMeleeAttackGoal(this, 1.3f,
                false, 50, true, true,
                "attack", "attack", 25, 18, this::onAttackStart)
                .setAreaAttack(true));
        this.goalSelector.addGoal(5, new BogreReturnToCauldronGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new BogreConditionalStrollGoal(this, 1.0D));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "hurt", 0, state -> PlayState.STOP)
                .triggerableAnim("hurt", RawAnimation.begin().then("taking_damage", Animation.LoopType.PLAY_ONCE)));
        controllerRegistrar.add(new AnimationController<>(this, "defaults_controller", 0, this::defaults).transitionLength(3));
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate)
                .setSoundKeyframeHandler(this::soundKeyframeHandler));
        controllerRegistrar.add(new AnimationController<>(this, "grab", 0, state -> PlayState.STOP)
                .triggerableAnim("grab", RawAnimation.begin().then("grab", Animation.LoopType.PLAY_ONCE)));
        controllerRegistrar.add(new AnimationController<>(this, "attack", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE)));
        controllerRegistrar.add(new AnimationController<>(this, "roar", 0, state -> PlayState.STOP)
                .triggerableAnim("roar", RawAnimation.begin().then("roar", Animation.LoopType.PLAY_ONCE)));
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
            
            int targetTicks = this.entityData.get(CARVE_DURATION);
            if (targetTicks > 0) {
                float speedMultiplier = 100.0f / targetTicks;
                animationState.getController().setAnimationSpeed(speedMultiplier);
            }
            
            if (animationState.getController().hasAnimationFinished()) {
                entityData.set(CARVING_ANIM, false);
                animationState.getController().forceAnimationReset();
                animationState.getController().setAnimationSpeed(1.0f); // reset speed
            }
            return PlayState.CONTINUE;
        }

        animationState.getController().setAnimationSpeed(1.0f);
        animationState.getController().forceAnimationReset();
        return PlayState.STOP;
    }

    private void soundKeyframeHandler(SoundKeyframeEvent<BogreEntity> event) {
        if (event.getKeyframeData().getSound().equals("hammer_sound")) {
            Player player = ClientUtils.getClientPlayer();
            if (player != null) {
                String customSound = this.entityData.get(HAMMER_SOUND);
                if (!customSound.isEmpty()) {
                    SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(customSound));
                    if (soundEvent != null) {
                        player.playSound(soundEvent, 1f, 0.8F + new Random().nextFloat() * 0.4F);
                        return;
                    }
                }
                
                if (this.getAIState() == State.CARVE_BONE) {
                    player.playSound(SoundEvents.BONE_BLOCK_HIT, 1f, 0.8F + new Random().nextFloat() * 0.4F);
                } else {
                    player.playSound(SoundEvents.ANVIL_LAND, .5f, 0.8F + new Random().nextFloat() * 0.4F);
                }
            }
        }
    }


    @Override
    public boolean doHurtTarget(@NotNull Entity target) {

        Vec3 offset = new Vec3(0, 0, 2); // offset to bone striking ground

        float yaw = this.getYRot();
        double rad = Math.toRadians(yaw);

        double rotatedX = offset.x * Math.cos(rad) - offset.z * Math.sin(rad);
        double rotatedZ = offset.x * Math.sin(rad) + offset.z * Math.cos(rad);
        Vec3 rotatedOffset = new Vec3(-rotatedX, offset.y, rotatedZ);

        Vec3 spawnPos = this.position().add(rotatedOffset);
        ShockwaveManager.addShockwave((ServerLevel) this.level(), spawnPos, SHOCKWAVE_DAMAGE, SHOCKWAVE_RADIUS, 40, this);
        return true;
    }

    private void onAttackStart(LivingEntity target) {
        this.playSound(ModSoundEvents.BOGRE_ATTACK.get(), 1, 1);
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
                this.setAIState(State.CAUTIOUS);
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

        if (this.getAIState() == State.CAUTIOUS) {
            if (isJukeboxPlayingNearby() && !entityData.get(DANCING)) {
                // start dancing
                entityData.set(DANCING, true);

                this.getNavigation().stop();
                this.setTarget(null);
                return;
            }

            cautiousAiStep();
        } else if (this.getAIState() == State.MAKE_CHOWDER) {
            makeChowderAiStep();
        } else if (this.getAIState() == State.CARVE_BONE) {
            carveBoneAiStep();
        } else if (this.getAIState() == State.MAKE_DISC) {
            carveDiscAiStep();
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
        // if attacked, switch to cautious state and set the attacker as target
        entityData.set(DANCING, false);
        this.setAIState(State.CAUTIOUS);
        if (pSource.getEntity() instanceof LivingEntity entity) {
            if (!this.getItemHeld().isEmpty()) {
                this.throwHeldItem();
            }

            // if the attacker is a player, remove them from tamedPlayers
            if (pSource.getEntity() instanceof Player player) {
                tamedPlayers.remove(player.getUUID());
            }

            this.setTarget(entity);
        }
        return result;
    }

    /**
     * The Bogre carves a giant bone from 3 bone blocks.
     * This is a placeholder method for future implementation.
     */
    private void carveBoneAiStep() {
        BogreCraftingManager.carveBoneAiStep(this);
    }

    /**
     * The Bogre makes a disc from a broken disc.
     * Goofy ahh code that is very similar to `carveBoneAiStep()`
     */
    private void carveDiscAiStep() {
        BogreCraftingManager.carveDiscAiStep(this);
    }

    private boolean isJukeboxPlayingNearby() {
        BlockPos origin = this.blockPosition();

        return BlockPos.betweenClosedStream(
                origin.offset(-JUKEBOX_RANGE, -2, -JUKEBOX_RANGE),
                origin.offset(JUKEBOX_RANGE, 2, JUKEBOX_RANGE)
        ).anyMatch(pos -> {
            BlockState state = level().getBlockState(pos);
            BlockEntity blockEntity = level().getBlockEntity(pos);
            return state.is(Blocks.JUKEBOX)
                    && blockEntity instanceof JukeboxBlockEntity jukeboxblockentity
                    && jukeboxblockentity.isRecordPlaying();
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
        BogreCraftingManager.makeChowderAiStep(this);
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
        // search for players within ROAR_RANGE
        if (!isRoaring()) {
            roaringTick = 0;
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

                    triggerAnim("roar", "roar");

                    warnedPlayers.add(player);

                    // play roar sound
                    this.level().playSound(null, this.blockPosition(), ModSoundEvents.BOGRE_ROAR.get(),
                            SoundSource.HOSTILE, 2.0F, 0.9F + this.level().random.nextFloat() * 0.2F);

                    break;
                }
            }
        } else {
            if (roaredPlayer != null && roaredPlayer.isAlive()) {
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

                    // roar effect particles
                    Vec3 add = new Vec3(getLookAngle().x, 0, getLookAngle().z).normalize().scale(2.25);
                    Vec3 mouthPos = new Vec3(getX(), getY() + 1.8, getZ()).add(add);
                    RoarEffectManager.addRoar((ServerLevel) level(), mouthPos, getLookAngle(), 45, 20);
                }
            } else {
                setRoaring(false);
                roaringTick = 0;
                roaredPlayer = null;
            }
        }

        // if roaring, keep looking at the roared player
        if (isRoaring() && roaredPlayer != null) {
            this.lookControl.setLookAt(roaredPlayer, 30.0F, 30.0F);
            return;
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

        if (this.getTarget() != null) {
            if (this.distanceTo(this.getTarget()) > FORGET_RANGE) {
                this.setTarget(null);
            }
        }

        if (this.getTarget() == null) {
            List<Player> withinHostileRange = this.level().getEntitiesOfClass(Player.class,
                    AABB.ofSize(position(), HOSTILE_RANGE * 2, HOSTILE_RANGE * 2, HOSTILE_RANGE * 2),
                    p -> !p.isSpectator() && distanceToSqr(p) <= HOSTILE_RANGE * HOSTILE_RANGE);

            // sort players by distance to the Bogre, closest to farthest
            withinHostileRange.sort((p1, p2) -> Float.compare(p1.distanceTo(this), p2.distanceTo(this)));
            for (Player player : withinHostileRange) {
                if (!this.isTamedBy(player) && this.hasLineOfSight(player)
                        && !player.isCreative() && !player.isSpectator()) {
                    this.setTarget(player);
                    break;
                }
            }
        }

        // prune warnedPlayers list
        warnedPlayers.removeIf(player -> player.distanceTo(this) > FORGET_RANGE);

        if (this.cauldronPos == null || !this.isValidCauldron(this.cauldronPos)) {
            // if no cauldron is assigned, don't attempt to make chowder or carve bone
            return;
        }

        // detect fish dropped by non-hostile players
        List<Player> possibleFishDroppers = this.level().getEntitiesOfClass(Player.class,
                AABB.ofSize(position(), FORGET_RANGE * 2, FORGET_RANGE * 2, FORGET_RANGE * 2),
                p -> !p.isSpectator() && distanceToSqr(p) <= FORGET_RANGE * FORGET_RANGE);

        for (Player player : possibleFishDroppers) {
            float distance = player.distanceTo(this);
            if (distance > HOSTILE_RANGE || this.isTamedBy(player) || player.isCreative()) {
                // get nearby item entities (fish on ground)
                List<ItemEntity> nearbyItems = this.level().getEntitiesOfClass(ItemEntity.class,
                        player.getBoundingBox().inflate(4), // check small radius around player
                        item -> item.isAlive() && BogreRecipeManager.isCookingIngredient(item.getItem().getItem())
                );

                for (ItemEntity ingredient : nearbyItems) {
                    if (this.hasLineOfSight(ingredient)) {
                        Optional<BogreRecipe> recipeOpt = BogreRecipeManager.getCookingRecipe(ingredient.getItem().getItem());
                        if (recipeOpt.isPresent()) {
                            ingredient.setExtendedLifetime();
                            droppedIngredientItem = ingredient;
                            droppedIngredientPlayer = player; // store the player that dropped the fishh
                            this.setActiveRecipe(recipeOpt.get()); // cache recipe
                            this.setAIState(State.MAKE_CHOWDER); // change state to make chowder
                            return;
                        }
                    }
                }
            }
        }

        // check for bone blocks to carve
        List<BlockPos> boneBlockPositions = BogreCraftingManager.findCarvableBlocks(this, (int) ROAR_RANGE);
        if (boneBlockPositions != null && !boneBlockPositions.isEmpty()) {
            Player trustedPlayer = BogreCraftingManager.findNearbyTrustedPlayer(this, BogreCraftingManager.getAveragePosition(boneBlockPositions), 5);
            if (trustedPlayer != null) {
                Optional<BogreRecipe> recipeOpt = BogreRecipeManager.getCarvingRecipe(this.level().getBlockState(boneBlockPositions.get(0)).getBlock());
                if (recipeOpt.isPresent()) {
                    this.setActiveRecipe(recipeOpt.get()); // cache recipe + sync to client
                    this.carvePositions = boneBlockPositions;
                    this.setDroppedIngredientPlayer(trustedPlayer);
                    this.setAIState(State.CARVE_BONE);
                    this.resetCarveTicks();
                    return;
                }
            }
        }

        // check for broken disc to make Bogre disc
        ItemEntity disc = BogreCraftingManager.findBrokenDisc(this, (int) ROAR_RANGE);
        if (disc != null) {
            Player trustedPlayer = BogreCraftingManager.findNearbyTrustedPlayer(this, disc.blockPosition(), 5);
            if (trustedPlayer != null) {
                Optional<BogreRecipe> recipeOpt = BogreRecipeManager.getTransformationRecipe(disc.getItem().getItem());
                if (recipeOpt.isPresent()) {
                    this.setActiveRecipe(recipeOpt.get()); // cache recipe + sync to client
                    this.carvePositions = List.of(disc.blockPosition());
                    this.setDroppedIngredientPlayer(trustedPlayer);
                    this.setAIState(State.MAKE_DISC);
                    this.resetCarveTicks();
                    return;
                }
            }
        }
    }

    /**
     * Find the nearest broken disc item entity within range.
     */
    private ItemEntity findBrokenDisc(int range) {
        BlockPos origin = this.blockPosition();
        AABB searchBox = new AABB(origin).inflate(range);
        List<ItemEntity> discs = this.level().getEntitiesOfClass(
                ItemEntity.class,
                searchBox,
                item -> item.isAlive() && BogreRecipeManager.isTransformationIngredient(item.getItem().getItem())
        );
        if (discs.isEmpty()) return null;
        return discs.get(0);
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

    public boolean moveTo(BlockPos pos, double speed, boolean checkCauldronDistance) {
        if (checkCauldronDistance && this.cauldronPos != null && this.cauldronPos.distToCenterSqr(pos.getX(), pos.getY(), pos.getZ()) > MAX_CAULDRON_DIST_SQR) {
            return false;
        }
        this.getNavigation().moveTo(this.getNavigation().createPath(pos, 0), speed);
        return true;
    }

    public boolean moveTo(BlockPos pos, double speed) {
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
        entityData.define(AI_STATE, 0);
        entityData.define(HAMMER_SOUND, "");
        entityData.define(CARVE_DURATION, 130);
    }

    /**
     * Works on client and server.
     */
    public State getAIState() {
        int state = entityData.get(AI_STATE);
        return State.values()[state];
    }

    /**
     * Only call this in server-side code, the client does not have authority to change the server AI state.
     */
    public void setAIState(State state) {
        entityData.set(AI_STATE, state.ordinal());
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

    public void setItemHeld(ItemStack itemHeld) {
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
        this.spawnAtLocation(new ItemStack(ModItems.GIANT_BONE.get()));
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

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
    
    public void setActiveRecipe(BogreRecipe recipe) {
        this.activeRecipe = recipe;
        if (recipe != null) {
            this.entityData.set(CARVE_DURATION, recipe.timeTicks());
            if (recipe.hammerSound().isPresent()) {
                this.entityData.set(HAMMER_SOUND, recipe.hammerSound().get().getLocation().toString());
            } else {
                this.entityData.set(HAMMER_SOUND, "");
            }
        } else {
            this.entityData.set(CARVE_DURATION, 130);
            this.entityData.set(HAMMER_SOUND, "");
        }
    }
    
    public List<BlockPos> getCarvePositions() { return carvePositions; }
    public void setCarvePositions(List<BlockPos> positions) { this.carvePositions = positions; }
    public BogreRecipe getActiveRecipe() { return activeRecipe; }
    public int getCarveTicks() { return carveTicks; }
    public void incrementCarveTicks() { this.carveTicks++; }
    public void resetCarveTicks() { this.carveTicks = 0; }
    public Player getDroppedIngredientPlayer() { return droppedIngredientPlayer; }
    public void setDroppedIngredientPlayer(Player player) { this.droppedIngredientPlayer = player; }
    public int getChowderThrowDelay() { return chowderThrowDelay; }
    public void setChowderThrowDelay(int delay) { this.chowderThrowDelay = delay; }
    public void decrementChowderThrowDelay() { this.chowderThrowDelay--; }
    public Vec3 getLastPos() { return lastPos; }
    public void setLastPos(Vec3 pos) { this.lastPos = pos; }
    public int getStuckTicks() { return stuckTicks; }
    public void incrementStuckTicks() { this.stuckTicks++; }
    public void resetStuckTicks() { this.stuckTicks = 0; }
    public boolean isPathSet() { return pathSet; }
    public void setPathSet(boolean pathSet) { this.pathSet = pathSet; }
    public Set<UUID> getTamedPlayers() { return tamedPlayers; }
    public ItemEntity getDroppedIngredientItem() { return droppedIngredientItem; }
    public void setDroppedIngredientItem(ItemEntity item) { this.droppedIngredientItem = item; }
    public int getCookingTicks() { return entityData.get(COOKING_TICKS); }
    public void setCookingTicks(int ticks) { entityData.set(COOKING_TICKS, ticks); }
    public void incrementCookingTicks() { setCookingTicks(getCookingTicks() + 1); }
    public void resetCookingTicks() { setCookingTicks(0); }
}
