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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

import java.util.*;
import java.util.function.Predicate;

public class BogreEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public enum State {
        CAUTIOUS,
        MAKE_CHOWDER,
        MAKE_BONE
    }

    public State state = State.CAUTIOUS;

    /**
     * On client side, this is used to start the roar animation and is set to false immediately after starting.
     * On server side, this is used to determine if the Bogre is currently roaring and is set to false after ROAR_TICKS
     */
    public static final EntityDataAccessor<Boolean> ROAR_ANIM = SynchedEntityData.defineId(BogreEntity.class, EntityDataSerializers.BOOLEAN);

    public static float FORGET_RANGE = 35f;
    public static float ROAR_RANGE = 24f;
    public static float HOSTILE_RANGE = 18f;

    private BlockPos cauldronPos = null;

    private static final int ROAR_TICKS = 100; // how long a roar animation lasts (server)

    private int roaringTick = 0;
    private Player roaredPlayer = null; // the player that the Bogre is currently roaring at
    private final List<Player> warnedPlayers = new ArrayList<>();

    // MAKE CHOWDER
    private boolean pickedUpFish = false; // if the Bogre has picked up the fish item
    private ItemEntity droppedFishItem = null;
    private Player droppedFishPlayer = null; // the player that dropped the fish item
    private static final double CHOWDER_REACH_DISTANCE = 3;
    private static final int CHOWDER_TIME_TICKS = 100;

    private int chowderTicks = 0;

    // list of players who have tamed the Bogre
    private final Set<UUID> tamedPlayers = new HashSet<>();


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
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
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

        if (animationState.isMoving()) {
            animationState.getController().setAnimation(RawAnimation.begin().then("walk", Animation.LoopType.LOOP));
        } else {
            animationState.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
        }

        return PlayState.CONTINUE;
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

        // if attacked during chowder making
        if (this.getLastHurtByMob() != null) {
            this.state = State.CAUTIOUS;
            if (this.getLastHurtByMob() instanceof Player player) {
                // if the attacker is a player, remove them from tamedPlayers
                if (tamedPlayers.contains(player.getUUID())) {
                    player.sendSystemMessage(Component.literal("The Bogre does not trust you anymore!"));
                }
                tamedPlayers.remove(player.getUUID());
            }
            this.setTarget(this.getLastHurtByMob());
            return;
        }
        if (this.state == State.CAUTIOUS) {
            cautiousAiStep();
        } else if (this.state == State.MAKE_CHOWDER) {
            makeChowderAiStep();
        }
    }

    /**
     * The Bogre attempts to make chowder from a fish dropped by a player.
     * The fish should be droppedFishItem.
     */
    private void makeChowderAiStep() {
        // if attacked during chowder making
        if (this.getLastHurtByMob() != null) {
            this.state = State.CAUTIOUS;
            this.setTarget(this.getLastHurtByMob());
            return;
        }

        if (pickedUpFish) {
            if (cauldronPos == null) { // TODO: if the bogre has no cauldron assigned, it will not attempt to make chowder
                this.state = State.CAUTIOUS;
                return;
            }
            double distance = this.distanceToSqr(cauldronPos.getX() + 0.5, cauldronPos.getY(), cauldronPos.getZ() + 0.5);
            distance = Math.sqrt(distance);
            if (distance > 2.5) {
                // move to the cauldron if not close enough
                this.getNavigation().moveTo(cauldronPos.getX() + 0.5, cauldronPos.getY(), cauldronPos.getZ() + 0.5, 1.0D);
                return;
            }

            // close enough to start making chowder
            this.navigation.stop();
            Inhabitants.LOGGER.debug("TICKING!");
            this.lookAt(EntityAnchorArgument.Anchor.FEET, cauldronPos.getCenter());
            this.lookAt(EntityAnchorArgument.Anchor.EYES, cauldronPos.getCenter());
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
                pickedUpFish = false; // reset the picked up fish state
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
        if (distance > CHOWDER_REACH_DISTANCE) {
            boolean path = this.getNavigation().moveTo(droppedFishItem.getX(), droppedFishItem.getY(), droppedFishItem.getZ(), 1.0D); // approach the fish
            if (!path) {
                Inhabitants.LOGGER.debug("Bogre failed to path to fish, distance: {}", distance);
            }
            return;
        }

        if (!pickedUpFish) {
            this.getNavigation().stop();
            droppedFishItem.discard();
            Inhabitants.LOGGER.debug("Picked up the fish");
            pickedUpFish = true; // mark that the Bogre has picked up the fish
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
                            break; // only trigger once per tick per player
                        }
                    }
                }
            }
        }
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
    }

    public boolean isTamedBy(Player player) {
        return tamedPlayers.contains(player.getUUID());
    }

    private boolean isValidCauldron(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        return state.getBlock() instanceof CauldronBlock;
    }

    @Override
    public float getStepHeight() {
        return 1.5f;
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

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
