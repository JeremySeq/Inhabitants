package com.jeremyseq.inhabitants.entities.bogre;

import com.jeremyseq.inhabitants.entities.EntityUtil;
import com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron.BogreCauldronEntity;
import com.jeremyseq.inhabitants.items.ModItems;
import com.jeremyseq.inhabitants.entities.bogre.ai.*;
import com.jeremyseq.inhabitants.entities.bogre.render.BogreAnimationHandler;
import com.jeremyseq.inhabitants.entities.bogre.utilities.*;
import com.jeremyseq.inhabitants.entities.bogre.render.RoarEffectRenderer;
import com.jeremyseq.inhabitants.entities.bogre.skill.BogreSkills;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.particles.ParticleTypes;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Bogre Entity
 * AI: BogreAi
 * Skills: CookingSkill, CarvingSkill, TransformationSkill
 */
public class BogreEntity extends Monster implements GeoEntity {

    // --- Core Systems & Helpers ---
    private final BogreAi ai = new BogreAi(this);
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public BogreAi getAi() { return ai; }

    // --- Synced Data Accessors ---
    private static <T> EntityDataAccessor<T> defineAccessor(EntityDataSerializer<T> serializer) {
        return SynchedEntityData.defineId(BogreEntity.class, serializer);
    }

    public static final EntityDataAccessor<Boolean> COOKING_ANIM = defineAccessor(EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> CARVING_ANIM = defineAccessor(EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> AI_STATE = defineAccessor(EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> NEUTRAL_STATE = defineAccessor(EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> AGGRESSIVE_STATE = defineAccessor(EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> CRAFTING_STATE = defineAccessor(EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> HAMMER_SOUND = defineAccessor(EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> SKILL_DURATION = defineAccessor(EntityDataSerializers.INT);
    public static final EntityDataAccessor<ItemStack> ITEM_HELD = defineAccessor(EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<Integer> ANIMATION_PHASE = defineAccessor(EntityDataSerializers.INT); // 0: Start, 1: Loop, 2: End
    public static final EntityDataAccessor<BlockPos> TARGET_POS = defineAccessor(EntityDataSerializers.BLOCK_POS);
    public static final EntityDataAccessor<Integer> TARGET_ENTITY_ID = defineAccessor(EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> SKILL_HITS = defineAccessor(EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> HAMMER_HITS = defineAccessor(EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> IS_TRANSFORMING_DISC = defineAccessor(EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> AI_TICKS = defineAccessor(EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DELIVERY_STATE = defineAccessor(EntityDataSerializers.INT);

    // --- State & Position Info ---
    public BlockPos cauldronPos = null;
    public BlockPos entrancePos = null;
    public BogreNeutralGoal.DancePhase dancePhase = BogreNeutralGoal.DancePhase.NONE; // Client-side only
    public boolean randomChance = false;
    
    // Client-side visual tracking for hammer effects
    public int clientSkillHits = 0;
    public int hammerHideTicks = 0;
    public boolean isHammerHidden = false;
    public Vec3 clientMouthPos = null;

    private int roaringTick = 0;

    // --- Constructor & Initialization ---
    public BogreEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setPersistenceRequired();
        this.getNavigation().setCanFloat(true);

        // Deter walking near fire/lava or over the cauldron area
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 16.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 16.0F);
        this.setPathfindingMalus(BlockPathTypes.LAVA, 16.0F);
        
        ((GroundPathNavigation) this.getNavigation()).setAvoidSun(false);
        this.ai.registerGoals();
        this.moveControl = new BogreMoveControl(this);
    }

    public static AttributeSupplier setAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 300.0F)
                .add(Attributes.ATTACK_DAMAGE, 30F)
                .add(Attributes.ATTACK_SPEED, .5)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5F)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0F)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.MOVEMENT_SPEED, .17f).build();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new BogreReturnToCauldronGoal(this, 1.0D));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(COOKING_ANIM, false);
        entityData.define(CARVING_ANIM, false);
        entityData.define(ITEM_HELD, ItemStack.EMPTY);
        entityData.define(AI_STATE, 0);
        entityData.define(NEUTRAL_STATE, 0);
        entityData.define(AGGRESSIVE_STATE, 0);
        entityData.define(CRAFTING_STATE, 0);
        entityData.define(HAMMER_SOUND, "");
        entityData.define(SKILL_DURATION, 130);
        entityData.define(ANIMATION_PHASE, 0);
        entityData.define(TARGET_POS, BlockPos.ZERO);
        entityData.define(TARGET_ENTITY_ID, -1);
        entityData.define(SKILL_HITS, 0);
        entityData.define(HAMMER_HITS, 1);
        entityData.define(IS_TRANSFORMING_DISC, false);
        entityData.define(AI_TICKS, 0);
        entityData.define(DELIVERY_STATE, 0);
    }

    // --- Combat & Attack Interaction ---
    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        this.triggerShockwave();
        return true;
    }

    public void triggerShockwave() {
        if (this.level().isClientSide) return;
        if (this.isRoaring()) return;
        BogreUtil.triggerShockwave(this, BogreAttackGoal.SHOCKWAVE_DAMAGE, BogreAttackGoal.SHOCKWAVE_RADIUS);
    }

    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float amount) {
        boolean result = super.hurt(damageSource, amount);
        if (result && !level().isClientSide) {
            this.triggerAnim("hurt", "hurt");
        }
        
        if (damageSource.getEntity() instanceof LivingEntity entity) {
            // never become aggressive towards creative or spectator players
            if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
                return result;
            }

            if (!this.getItemHeld().isEmpty()) {
                this.throwHeldItem();
            }

            if (entity instanceof Player player) {
                BogreAttackGoal attackGoal = getAttackGoal();
                if (attackGoal != null) {
                    attackGoal.getAttackedByPlayers().add(player.getUUID());
                }
                
                this.setTarget(entity);
                if (this.getAIState() != BogreAi.State.AGGRESSIVE) {
                    if (attackGoal != null) attackGoal.enterRoaring(player);
                } else {
                    if (attackGoal != null) attackGoal.enterAttacking();
                }
            } else {
                this.setTarget(entity);
                BogreAttackGoal attackGoal = getAttackGoal();
                if (attackGoal != null) attackGoal.enterAttacking();
            }
        }
        return result;
    }

    public @Nullable BogreAttackGoal getAttackGoal() {
        return (BogreAttackGoal) this.goalSelector.getAvailableGoals().stream()
        .filter(g -> g.getGoal() instanceof BogreAttackGoal)
        .map(g -> (BogreAttackGoal) g.getGoal())
        .findFirst()
        .orElse(null);
    }

    public @Nullable BogreNeutralGoal getNeutralGoal() {
        return (BogreNeutralGoal) this.goalSelector.getAvailableGoals().stream()
        .filter(g -> g.getGoal() instanceof BogreNeutralGoal)
        .map(g -> (BogreNeutralGoal) g.getGoal())
        .findFirst()
        .orElse(null);
    }

    // --- AI Tick & Logic ---
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.isRoaring()) {
                if (roaringTick == 10) {
                    RoarEffectRenderer.addRoar(this, 35, 1.0);
                }
                if (roaringTick == 10 || roaringTick == 15) {
                    EntityUtil.screamParticles((ClientLevel) this.level(),
                    getMouthPos(), this.getLookAngle(), .5f);
                }
                roaringTick++;
            } else {
                roaringTick = 0;
            }

            if (this.hammerHideTicks > 0) {
                this.hammerHideTicks--;
                if (this.hammerHideTicks == 0) {
                    this.isHammerHidden = false;
                }
            }
        }
    }

    @Override
    public void die(@NotNull DamageSource pCause) {
        BogreSkills.CARVING.clearCracks(this);
        super.die(pCause);
    }

    @Override
    public void remove(Entity.@NotNull RemovalReason pReason) {
        BogreSkills.CARVING.clearCracks(this);
        super.remove(pReason);
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        ai.aiStep();
    }

    public BogreAi.State getAIState() {
        return BogreAi.State.values()[entityData.get(AI_STATE)];
    }

    public void setAIState(BogreAi.State state) {
        entityData.set(AI_STATE, state.ordinal());
    }

    public BogreAi.NeutralState getNeutralState() {
        return BogreAi.NeutralState.values()[entityData.get(NEUTRAL_STATE)];
    }

    public void setNeutralState(BogreAi.NeutralState state) {
        entityData.set(NEUTRAL_STATE, state.ordinal());
    }

    public BogreAi.AggressiveState getAggressiveState() {
        return BogreAi.AggressiveState.values()[entityData.get(AGGRESSIVE_STATE)];
    }

    public void setAggressiveState(BogreAi.AggressiveState state) {
        entityData.set(AGGRESSIVE_STATE, state.ordinal());
    }

    public BogreAi.SkillingState getCraftingState() {
        return BogreAi.SkillingState.values()[entityData.get(CRAFTING_STATE)];
    }

    public void setCraftingState(BogreAi.SkillingState state) {
        entityData.set(CRAFTING_STATE, state.ordinal());
    }

    // --- Hand Interaction & Items ---
    public ItemStack getItemHeld() {
        return this.entityData.get(ITEM_HELD);
    }

    public void setItemHeld(ItemStack itemHeld) {
        setItemHeld(itemHeld, true);
    }

    public void setItemHeld(ItemStack itemHeld, boolean playSound) {
        if (playSound) this.playSound(SoundEvents.ITEM_PICKUP, 1, 1);
        this.entityData.set(ITEM_HELD, itemHeld);
    }

    private boolean isHoldingChowder() {
        return this.getItemHeld().is(ModItems.FISH_SNOT_CHOWDER.get())
                || this.getItemHeld().is(ModItems.UNCANNY_POTTAGE.get())
                || this.getItemHeld().is(Items.SUSPICIOUS_STEW);
    }

    public void throwHeldItem() {
        assert !this.level().isClientSide();
        this.triggerAnim("trigger_controller", "grab");
        EntityUtil.throwItemStack(this.level(), this, this.getItemHeld(), .3f, 0);
        setItemHeld(ItemStack.EMPTY, false);
    }



    @Override
    public void handleEntityEvent(byte id) {
        super.handleEntityEvent(id);
    }

    // --- Cooking & Cauldron ---
    public int getCookingTicks() {
        return getAiTicks();
    }

    public void setCookingTicks(int ticks) {
        setAiTicks(ticks);
    }

    public void incrementCookingTicks() {
        incrementAiTicks();
    }

    public void resetCookingTicks() {
        resetAiTicks();
    }

    public BogreCauldronEntity getCauldronEntity() {
        if (this.cauldronPos == null) return null;
        List<BogreCauldronEntity> entities = this.level().getEntitiesOfClass(
                BogreCauldronEntity.class, new AABB(cauldronPos), entity -> !entity.isRemoved());
        return entities.isEmpty() ? null : entities.get(0);
    }

    public boolean isValidCauldron(BlockPos pos) {
        return BogreDetectionHelper.isValidCauldron(this.level(), pos);
    }

    public boolean canSeeCauldron() {
        return BogreDetectionHelper.canSeeCauldron(this);
    }

    // --- Navigation & Movement Helpers ---
    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level pLevel) {
        return new BogrePathNavigation(this, pLevel);
    }

    // --- Sound & Anim Helpers ---
    public boolean isRoaring() { 
        return getAIState() == BogreAi.State.AGGRESSIVE &&
        getAggressiveState() == BogreAi.AggressiveState.ROARING; 
    }
    public void setRoaring(boolean roaring) { 
        // Logic handled via AI states
    }

    public Vec3 getMouthPos() {
        if (this.level().isClientSide && clientMouthPos != null) {
            return clientMouthPos;
        }
        Vec3 look = getMouthDirection();
        Vec3 add = new Vec3(look.x, 0, look.z).normalize().scale(2.25);
        return new Vec3(this.getX(), this.getY() + 1.8, this.getZ()).add(add);
    }

    public Vec3 getMouthDirection() {
        return this.getLookAngle().normalize();
    }

    @Override
    protected SoundEvent getAmbientSound() { return BogreSoundHandler.getAmbientSound(); }
    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) { return BogreSoundHandler.getHurtSound(pDamageSource); }
    @Override
    protected @NotNull SoundEvent getDeathSound() { return BogreSoundHandler.getDeathSound(); }
    protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) { BogreSoundHandler.playStepSound(this, pPos, pState); }

    @Override
    public float getStepHeight() { return 1.5f; }
    @Override
    public int getHeadRotSpeed() { return 30; }
    @Override
    public int getMaxHeadYRot() { return 120; }
    @Override
    public boolean onClimbable() { return false; }
    @Override
    public boolean isPersistenceRequired() { return true; }

    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.GIANT_BONE.get()));
    }

    // --- NBT & Save Data ---
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        BogreNbtHelper.save(this, tag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        BogreNbtHelper.load(this, tag);
    }

    // --- Geckolib Implementation ---
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        BogreAnimationHandler.registerControllers(this, controllerRegistrar);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    // --- Refactoring Redirection (Fields moved to AI/Goal classes) ---
    public void setPathSet(boolean pathSet) { this.getAi().setPathSet(pathSet); }
    public void resetStuckTicks() { this.getAi().resetStuckTicks(); }
    public void setLastPos(Vec3 pos) { this.getAi().setLastPos(pos); }
    public Vec3 getLastPos() { return this.getAi().getLastPos(); }
    public int getStuckTicks() { return this.getAi().getStuckTicks(); }
    public void incrementStuckTicks() { this.getAi().incrementStuckTicks(); }

    public int getAiTicks() { return entityData.get(AI_TICKS); }
    public void setAiTicks(int ticks) { entityData.set(AI_TICKS, ticks); }
    public void incrementAiTicks() { setAiTicks(getAiTicks() + 1); }
    public void resetAiTicks() { setAiTicks(0); }
}

