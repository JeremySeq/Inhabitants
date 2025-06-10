package com.jeremyseq.inhabitants.entities.bogre;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
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

    public State state = State.CAUTIOUS;

    /**
     * On client side, this is used to start the roar animation and is set to false immediately after starting.
     * On server side, this is used to determine if the Bogre is currently roaring and is set to false after ROAR_TICKS
     */
    public static final EntityDataAccessor<Boolean> ROAR_ANIM = SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> ATTACK_ANIM = SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> COOKING_ANIM = SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.BOOLEAN);

    public static float FORGET_RANGE = 35f;
    public static float ROAR_RANGE = 24f;
    public static float HOSTILE_RANGE = 18f;
    public static final double MAX_CAULDRON_DIST_SQR = 24*24;

    public BlockPos cauldronPos = null;

    private static final int ROAR_TICKS = 100; // how long a roar animation lasts (server)

    private int roaringTick = 0;
    private Player roaredPlayer = null; // the player that the Bogre is currently roaring at
    private final List<Player> warnedPlayers = new ArrayList<>();

    // CARVE_BONE
    private static final int BONE_CARVE_TIME_TICKS = 100; // time it takes to carve a bone
    private int boneCarveTicks = 0; // counts down while carving a bone
    private List<BlockPos> boneBlockPositions; // the positions of the bone blocks to carve

    // MAKE CHOWDER
//    public static final EntityDataAccessor<Boolean> HOLDING_FISH = // if the Bogre has picked up the fish item
//            SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<ItemStack> FISH_HELD = // if the Bogre has picked up the fish item
            SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.ITEM_STACK);

    private ItemEntity droppedFishItem = null;
    private Player droppedFishPlayer = null; // the player that dropped the fish item
    private static final double FISH_REACH_DISTANCE = 3;
    private static final int CHOWDER_TIME_TICKS = 280;
    private static final int DROP_FISH_OFFSET = 70; // at what time the Bogre should drop the fish item after starting to make chowder

    private double chowderTicks = 0; // used both on client and server to time cooking

    // list of players who have tamed the Bogre
    private final Set<UUID> tamedPlayers = new HashSet<>();


    // ATTACK
    private int attackCooldown = 0;
    private int attackWindup = -1;
    private int attackPostDelay = -1; // counts down after damage, finishing attack animation

    private static final int ATTACK_COOLDOWN_TICKS = 60; // time between attacks
    private static final int ATTACK_OFFSET = 25; // windup time before damage


    private boolean randomChance = false; // used to trigger a rare idle animation

    public BogreEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier setAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 400.0F)
                .add(Attributes.ATTACK_DAMAGE, 30F)
                .add(Attributes.ATTACK_SPEED, 1.0f)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5F)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0F)
                .add(Attributes.MOVEMENT_SPEED, .25f).build();
    }

    @Override
    protected void registerGoals() {
//        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
//        this.goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 1.0D, true, 4, () -> true));
        this.goalSelector.addGoal(7, new ConditionalStrollGoal(this, 1.0D));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> animationState) {
        if (isRoaring()) {
            animationState.getController().setAnimation(RawAnimation.begin().then("roar", Animation.LoopType.PLAY_ONCE));
            if (animationState.getController().hasAnimationFinished()) {
                setRoaring(false);
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

        if (entityData.get(ATTACK_ANIM)) {
            animationState.getController().setAnimation(RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE));
            if (animationState.getController().hasAnimationFinished()) {
                entityData.set(ATTACK_ANIM, false);
                animationState.getController().forceAnimationReset();
            }
            return PlayState.CONTINUE;
        }

        if (animationState.isMoving()) {
            animationState.getController().setAnimation(RawAnimation.begin().then("walk", Animation.LoopType.LOOP));
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

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.entityData.get(COOKING_ANIM)) {
                chowderTicks++;
            } else {
                chowderTicks = 0;
            }
        }
    }

    @Override
    public void customServerAiStep() {
        if (cauldronPos == null || !isValidCauldron(cauldronPos)) {
            Optional<BlockPos> nearestCauldron = BlockPos.betweenClosedStream(blockPosition().offset(-10, -2, -10), blockPosition().offset(10, 2, 10))
                    .map(BlockPos::immutable)
                    .filter(pos -> level().getBlockState(pos).getBlock() instanceof CauldronBlock)
                    .findFirst();

            nearestCauldron.ifPresent(blockPos -> cauldronPos = blockPos);
            Inhabitants.LOGGER.debug("Bogre assigned to cauldron at: {}", cauldronPos);
        }

        if (this.getTarget() != null) {
            if (this.distanceTo(this.getTarget()) > FORGET_RANGE || !this.getTarget().isAlive() || this.getTarget().isDeadOrDying()) {
                this.setTarget(null);
            }
        }

        // attack target
        if (attackCooldown > 0 || attackWindup > 0 || this.getTarget() != null) {
            attackPlayerAiStep();
            return;
        }

        if (this.state == State.CAUTIOUS) {
            cautiousAiStep();
        } else if (this.state == State.MAKE_CHOWDER) {
            makeChowderAiStep();
        } else if (this.state == State.CARVE_BONE) {
            carveBoneAiStep();
        }
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        // if attacked by a player, switch to cautious state and set the player as target
        this.state = State.CAUTIOUS;
        if (pSource.getEntity() instanceof Player player && player.isAlive()) {
            // if the attacker is a player, remove them from tamedPlayers
            if (tamedPlayers.contains(player.getUUID())) {
                player.sendSystemMessage(Component.literal("The Bogre does not trust you anymore!"));
            }
            tamedPlayers.remove(player.getUUID());
            this.setTarget(player);
        }
        return super.hurt(pSource, pAmount);
    }

    /**
     * Attacks the target.
     */
    private void attackPlayerAiStep() {
        LivingEntity target = this.getTarget();

        // cancel target if it's invalid
        if (target != null && (!target.isAlive() || target.isDeadOrDying())) {
            this.setTarget(null);
            target = null;
        }

        // tick down cooldown
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        // handle animationn finish even if no target
        if (attackPostDelay > 0) {
            attackPostDelay--;
            if (attackPostDelay == 0) {
                entityData.set(ATTACK_ANIM, false);
            }
            return; // still finishing the animation
        }

        // handle windup - but only if a valid target is still around
        if (attackWindup > 0) {
            attackWindup--;
            if (attackWindup == 0) {
                if (target != null) {
                    double attackReach = this.getBbWidth() * 1.5f + target.getBbWidth();
                    double distanceSq = this.distanceToSqr(target);
                    if (distanceSq <= attackReach * attackReach) {
                        this.doHurtTarget(target);
                        this.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
                    }
                }
                attackPostDelay = 15; // let animation finish
                attackCooldown = ATTACK_COOLDOWN_TICKS;
                attackWindup = -1;
            }
            return;
        }

        // if no target, we're done for this tick
        if (target == null) {
            return;
        }

        double attackReach = this.getBbWidth() * 1.5f + target.getBbWidth();
        double distanceSq = this.distanceToSqr(target);

        // if close enough and off cooldown, start windup
        if (distanceSq <= attackReach * attackReach && attackCooldown <= 0 && attackWindup < 0) {
            attackWindup = ATTACK_OFFSET;
            entityData.set(ATTACK_ANIM, false); // reset to allow retrigger
            entityData.set(ATTACK_ANIM, true);
            return;
        }

        // approach the target if not close enough
        BlockPos targetPos = new BlockPos((int) target.getX(), (int) target.getY(), (int) target.getZ());
        this.moveTo(targetPos, 1);
    }


    /**
     * The Bogre carves a giant bone from 3 bone blocks.
     * This is a placeholder method for future implementation.
     */
    private void carveBoneAiStep() {
        // find/verify bone blocks
//        List<BlockPos> boneBlockPositions = findThreeBoneBlocksInLine((int) ROAR_RANGE);

        if (boneBlockPositions == null || boneBlockPositions.size() < 3) {
            this.state = State.CAUTIOUS; // revert to cautious state if not enough bone blocks found
            return;
        }

        // ensure the bone blocks are still valid
        for (BlockPos pos : boneBlockPositions) {
            BlockState state = this.level().getBlockState(pos);
            if (!state.is(Blocks.BONE_BLOCK)) {
                Inhabitants.LOGGER.debug("Bogre found invalid bone block at: {}", pos);
                this.state = State.CAUTIOUS; // revert to cautious state if any block is not a bone block
                return;
            }
        }

        // move to the carving site
        BlockPos center = getAveragePosition(boneBlockPositions);
        double distance = this.distanceToSqr(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        distance = Math.sqrt(distance);
        if (distance > 2.5) {
            this.moveTo(center, 1);
//            this.getNavigation().moveTo(this.getNavigation().createPath(center, 0), 1);
            return;
        }

        // carving
        this.getNavigation().stop();
        this.lookAt(EntityAnchorArgument.Anchor.FEET, Vec3.atCenterOf(center));
        this.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(center));
        boneCarveTicks++;

        if (boneCarveTicks >= BONE_CARVE_TIME_TICKS) {
            Inhabitants.LOGGER.debug("BONE CARVING COMPLETE!");

            // remove the bone blocks
            for (BlockPos pos : boneBlockPositions) {
                if (this.level().getBlockState(pos).is(Blocks.BONE_BLOCK)) {
                    this.level().destroyBlock(pos, false); // remove block without drops
                }
            }

            this.spawnAtLocation(new ItemStack(ModItems.GIANT_BONE.get()));
            this.playSound(SoundEvents.STONE_BREAK, 1.0F, 0.7F); // some kind of cracking/carving sound

            this.state = State.CAUTIOUS;
            boneCarveTicks = 0;
        }
    }

    /**
     * The Bogre attempts to make chowder from a fish dropped by a player.
     * The fish should be droppedFishItem.
     */
    private void makeChowderAiStep() {
        if (!this.getFishHeld().isEmpty()) {
            if (cauldronPos == null) { // TODO: if the bogre has no cauldron assigned, it will not attempt to make chowder
                this.state = State.CAUTIOUS;
                return;
            }
            double distance = this.distanceToSqr(cauldronPos.getX() + 0.5, cauldronPos.getY(), cauldronPos.getZ() + 0.5);
            distance = Math.sqrt(distance);
            if (distance > 2.5) {
                // move to the cauldron if not close enough
                this.moveTo(cauldronPos, 1);
//                this.getNavigation().moveTo(cauldronPos.getX() + 0.5, cauldronPos.getY(), cauldronPos.getZ() + 0.5, 1.0D);
                return;
            }

            // close enough to start making chowder
            this.navigation.stop();
            Inhabitants.LOGGER.debug("TICKING!");
            this.lookAt(EntityAnchorArgument.Anchor.FEET, cauldronPos.getCenter());
            this.lookAt(EntityAnchorArgument.Anchor.EYES, cauldronPos.getCenter());
            // start cooking animation
            if (chowderTicks == 0) {
                Inhabitants.LOGGER.debug("STARTING CHOWDER ANIMATION!");
                entityData.set(COOKING_ANIM, false);
                entityData.set(COOKING_ANIM, true);
            }
            chowderTicks++;
            if (chowderTicks >= CHOWDER_TIME_TICKS) {
                Inhabitants.LOGGER.debug("CHOWDER COMPLETE!");

                // chowder complete
                // TODO: add particle effects or something?
                this.spawnAtLocation(new ItemStack(ModItems.FISH_SNOT_CHOWDER.get())); // spawn the chowder item

                droppedFishPlayer.sendSystemMessage(Component.literal("The Bogre has made chowder from the fish you dropped!"));
                if (!tamedPlayers.contains(droppedFishPlayer.getUUID())) {
                    droppedFishPlayer.sendSystemMessage(Component.literal("You have tamed the Bogre!"));
                }
                this.playSound(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 1.0F, 0.8F); // play something watery?
                tamedPlayers.add(droppedFishPlayer.getUUID()); // add the player that dropped the fish to the tamed list
                this.setFishHeld(ItemStack.EMPTY); // reset the holding fish state
                droppedFishPlayer = null; // reset the dropped fish player
                this.state = State.CAUTIOUS;
                chowderTicks = 0;
            }

            return;
        }

        if (droppedFishItem == null || !droppedFishItem.isAlive()) {
            this.state = State.CAUTIOUS;
            return;
        }

        double distance = this.distanceTo(droppedFishItem);
        if (distance > FISH_REACH_DISTANCE) {
            BlockPos pos = new BlockPos(droppedFishItem.getBlockX(), droppedFishItem.getBlockY(), droppedFishItem.getBlockZ());
            this.moveTo(pos, 1, false);
            return;
        }

        if (this.getFishHeld().isEmpty()) {
            this.getNavigation().stop();
            ItemStack fishStack = droppedFishItem.getItem();
            Item fishItem = fishStack.getItem();

            if (fishStack.getCount() > 1) {
                fishStack.shrink(1); // remove only one fish
            } else {
                droppedFishItem.discard(); // discard the item if it was the last fish
            }

            Inhabitants.LOGGER.debug("Picked up one fish");
            this.setFishHeld(new ItemStack(fishItem, 1)); // set the holding fish state
            droppedFishItem = null; // reset the dropped fish item
            return;
        }
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
                if (!this.isTamedBy(player) && !warnedPlayers.contains(player) && player.distanceTo(this) <= ROAR_RANGE && this.hasLineOfSight(player)) {
                    // face player and start roaring
                    roaredPlayer = player;
                    this.lookControl.setLookAt(roaredPlayer);
                    player.sendSystemMessage(Component.literal("The Bogre roars at you!"));
                    setRoaring(true);
                    warnedPlayers.add(player);
                    break;
                }
            }
        } else {
            this.lookControl.setLookAt(roaredPlayer);
            roaringTick++;
            if (roaringTick >= ROAR_TICKS) {
                setRoaring(false);
                roaredPlayer = null; // reset the roared player
                roaringTick = 0;
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
                    getBoundingBox().inflate(FORGET_RANGE), Predicate.not(Player::isSpectator));

            if (!nearbyPlayers.isEmpty()) {
                nearbyPlayers.sort((a, b) -> Float.compare(a.distanceTo(this), b.distanceTo(this)));
                Player nearest = nearbyPlayers.get(0);

                if (this.hasLineOfSight(nearest)) {
                    this.lookControl.setLookAt(nearest, 30.0F, 30.0F);
                }
            }
        }

        // detect fish dropped by non-hostile players
        List<Player> possibleFishDroppers = this.level().getEntitiesOfClass(Player.class,
                getBoundingBox().inflate(FORGET_RANGE), Predicate.not(Player::isSpectator));

        for (Player player : possibleFishDroppers) {
            float distance = player.distanceTo(this);
            if (distance <= FORGET_RANGE) {
                if (distance > HOSTILE_RANGE || this.isTamedBy(player)) {
                    // get nearby item entities (fish on ground)
                    List<ItemEntity> nearbyItems = this.level().getEntitiesOfClass(ItemEntity.class,
                            player.getBoundingBox().inflate(4), // check small radius around player
                            item -> item.isAlive() &&
                                    (item.getItem().is(Items.COD)
                                            || item.getItem().is(Items.SALMON)
                                            || item.getItem().is(Items.TROPICAL_FISH)
                                            || item.getItem().is(Items.PUFFERFISH)
                                    ) // TODO: any fish item, use a tag or something
                    );

                    for (ItemEntity fishItem : nearbyItems) {
                        if (this.hasLineOfSight(fishItem)) {
                            player.sendSystemMessage(Component.literal("The Bogre notices the fish you dropped..."));

                            fishItem.setExtendedLifetime();
                            droppedFishItem = fishItem;
                            droppedFishPlayer = player; // store the player that dropped the fish
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

    private boolean moveTo(BlockPos pos, double speed, boolean checkCauldronDistance) {
        if (checkCauldronDistance && this.cauldronPos.distToCenterSqr(pos.getX(), pos.getY(), pos.getZ()) > MAX_CAULDRON_DIST_SQR) {
            Inhabitants.LOGGER.debug("Bogre is too far from cauldron, not moving to position: {}", pos);
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
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ROAR_ANIM, false);
        entityData.define(ATTACK_ANIM, false);
        entityData.define(COOKING_ANIM, false);
        entityData.define(FISH_HELD, ItemStack.EMPTY);
    }

    public boolean isTamedBy(Player player) {
        return tamedPlayers.contains(player.getUUID());
    }

    private boolean isValidCauldron(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        return state.getBlock() instanceof CauldronBlock;
    }

    public ItemStack getFishHeld() {
        return this.entityData.get(FISH_HELD);
    }

    /**
     * Used specifically by the client to determine what to render in the Bogre's hand.
     */
    public ItemStack getAnimateFishHeld() {
        if (!this.getFishHeld().isEmpty() && this.entityData.get(COOKING_ANIM)) {
            // if the chowder animation is at the drop fish offset, return an empty stack
            if (this.chowderTicks >= DROP_FISH_OFFSET) {
                return ItemStack.EMPTY;
            }
        }
        return this.getFishHeld();
    }
    
    private void setFishHeld(ItemStack fishHeld) {
        this.entityData.set(FISH_HELD, fishHeld);
    }

    @Override
    public float getStepHeight() {
        return 1.5f;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.BRACER_OF_MIGHT.get()));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
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
    public void readAdditionalSaveData(CompoundTag tag) {
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


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
