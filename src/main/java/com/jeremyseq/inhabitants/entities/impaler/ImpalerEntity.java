package com.jeremyseq.inhabitants.entities.impaler;

import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import com.jeremyseq.inhabitants.damagesource.ModDamageTypes;
import com.jeremyseq.inhabitants.entities.EntityUtil;
import com.jeremyseq.inhabitants.entities.goals.BreakTorchGoal;
import com.jeremyseq.inhabitants.entities.goals.SprintAtTargetGoal;
import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.keyframe.event.CustomInstructionKeyframeEvent;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;
import software.bernie.geckolib.core.object.PlayState;

import java.util.Random;

public class ImpalerEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(ImpalerEntity.class, EntityDataSerializers.INT);

    private int attackAnimTimer = 0;

    public ImpalerEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setMaxUpStep(1.5f);
    }

    public enum Variant {
        DEFAULT(0),
        DRIPSTONE(1),
        ALBINO(2),
        FORLORN_HOLLOWS(3);

        private final int id;

        Variant(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    public static AttributeSupplier setAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50f)
                .add(Attributes.ATTACK_DAMAGE, 8f)
                .add(Attributes.ATTACK_SPEED, 1.0f)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5F)
                .add(Attributes.FOLLOW_RANGE, 30f)
                .add(Attributes.MOVEMENT_SPEED, .25f).build();
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 30f, 1));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.addBehaviourGoals();
    }

    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RestrictSunGoal(this));
        this.goalSelector.addGoal(2, new FleeSunGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new ImpalerSpikeThrowGoal(this));
        this.goalSelector.addGoal(4, new ImpalerScreamGoal(this));
        this.goalSelector.addGoal(5, new SprintAtTargetGoal(this, 1.4D, 7, 3));
        this.goalSelector.addGoal(6, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(7, new BreakTorchGoal(this, 1));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
    }

    @Override
    public void tick() {
        super.tick();

        // regenerate health over time
        if (this.getTarget() == null && this.tickCount % 60 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(1.0F);
        }
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.IMPALER_SPIKE.get(), new Random().nextInt(2, 4)));

        if (source.getEntity() instanceof Creeper creeper &&
            creeper.isPowered()) {

            Item head = switch (this.getVariant()) {
                case DEFAULT -> ModItems.IMPALER_HEAD.get();
                case DRIPSTONE -> ModItems.IMPALER_HEAD_DRIPSTONE.get();
                case ALBINO -> ModItems.IMPALER_HEAD_ALBINO.get();
                case FORLORN_HOLLOWS -> ModItems.IMPALER_HEAD_FORLORN_HOLLOWS.get();
            };

            this.spawnAtLocation(head);
        }
    }

    public void aiStep() {
        // burn in sunlight
        if (this.isAlive()) {
            boolean flag = this.isSunBurnTick();
            if (flag) {
                ItemStack itemstack = this.getItemBySlot(EquipmentSlot.HEAD);
                if (!itemstack.isEmpty()) {
                    if (itemstack.isDamageableItem()) {
                        itemstack.setDamageValue(itemstack.getDamageValue() + this.random.nextInt(2));
                        if (itemstack.getDamageValue() >= itemstack.getMaxDamage()) {
                            this.broadcastBreakEvent(EquipmentSlot.HEAD);
                            this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                        }
                    }

                    flag = false;
                }

                if (flag) {
                    this.setSecondsOnFire(8);
                }
            }
        }

        super.aiStep();
    }

    @Override
    protected void customServerAiStep() {
        if (this.attackAnimTimer > 0) {
            this.attackAnimTimer--;
            if (this.attackAnimTimer == 0) {
                LivingEntity target = getTarget();
                if (target != null && distanceToSqr(target) <= this.getMeleeAttackRangeSqr(target)) {
                    target.hurt(ModDamageTypes.causeImpaledDamage(
                        this.level(), this),
                        (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE));
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        if (!level().isClientSide) {
            triggerAnim("attack", "bite");
            this.playSound(ModSoundEvents.IMPALER_ATTACK.get(), 1, 1);
            this.attackAnimTimer = 10;
        }
        return true;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {

        // prevent impaler's from hurting each other
        if (source.getEntity() instanceof ImpalerEntity) return false;

        // immune to stalactite + stalagmite damage
        if (source.is(DamageTypes.FALLING_STALACTITE) || source.is(DamageTypes.STALAGMITE)) {
            return false;
        }

        boolean result = super.hurt(source, amount);
        if (result && !level().isClientSide) {
            this.triggerAnim("hurt", "hurt");
        }

        return result;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof ImpalerEntity) && super.canAttack(target);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::predicate));
        controllers.add(new AnimationController<>(this, "hurt", 0, state -> PlayState.STOP)
                .triggerableAnim("hurt", RawAnimation.begin().then("hurt", Animation.LoopType.PLAY_ONCE)));
        controllers.add(new AnimationController<>(this, "attack", 0, state -> PlayState.STOP)
                .triggerableAnim("bite", RawAnimation.begin().then("bite", Animation.LoopType.PLAY_ONCE)));
        controllers.add(new AnimationController<>(this, "spike throw", 0, state -> PlayState.STOP)
                .triggerableAnim("spike throw", RawAnimation.begin().then("spike throw", Animation.LoopType.PLAY_ONCE))
                .setSoundKeyframeHandler(ImpalerEntity::handleSpikeThrowKeyframe)
        );
        controllers.add(new AnimationController<>(this, "scream", 0, state -> PlayState.STOP)
                .triggerableAnim("scream", RawAnimation.begin().then("scream", Animation.LoopType.PLAY_ONCE))
                .setCustomInstructionKeyframeHandler(ImpalerEntity::handleScreamKeyframe)
        );
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> animationState) {
        if (animationState.isMoving()) {
            if (this.isSprinting()) {
                animationState.setAndContinue(RawAnimation.begin().then("run", Animation.LoopType.LOOP));
            } else {
                animationState.setAndContinue(RawAnimation.begin().then("walk", Animation.LoopType.LOOP));
            }
        } else {
            animationState.setAndContinue(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
        }

        return PlayState.CONTINUE;
    }

    private static void handleSpikeThrowKeyframe(SoundKeyframeEvent<ImpalerEntity> event) {
        if (event.getKeyframeData().getSound().trim().equals("spikes")) {
            assert Minecraft.getInstance().level != null;
            Minecraft.getInstance().level.playSound(Minecraft.getInstance().player, event.getAnimatable().blockPosition(), ModSoundEvents.IMPALER_SPIKES.get(), SoundSource.HOSTILE);
        }
    }

    private static void handleScreamKeyframe(CustomInstructionKeyframeEvent<ImpalerEntity> event) {
        ImpalerEntity impaler = event.getAnimatable();

        EntityUtil.screamParticles((ClientLevel) impaler.level(),
                new Vec3(impaler.getX(), impaler.getY() + 0.5, impaler.getZ()),
                impaler.getLookAngle());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(VARIANT, Variant.DEFAULT.getId());
    }

    private Variant generateVariant() {
        // 5% chance to spawn with albino texture, otherwise default texture
        if (this.random.nextFloat() < 0.05f) {
            return Variant.ALBINO;
        }

        if (this.level().getBiome(this.blockPosition()).is(Biomes.DRIPSTONE_CAVES)) {
            return Variant.DRIPSTONE;
        }

        // alex's caves forlorn hollows compat
        if (ModList.get().isLoaded("alexscaves")) {
            ResourceKey<Biome> FORLORN_HOLLOW_BIOME =
                    ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("alexscaves", "forlorn_hollows"));

            if (this.level().getBiome(this.blockPosition()).is(FORLORN_HOLLOW_BIOME)) {
                return Variant.FORLORN_HOLLOWS;
            }
        }

        // basic ahh impaler
        return Variant.DEFAULT;
    }

    public Variant getVariant() {
        return Variant.values()[this.entityData.get(VARIANT)];
    }

    public void setVariant(Variant v) {
        this.entityData.set(VARIANT, v.getId());
    }
    public void setVariant(int id) {
        this.entityData.set(VARIANT, id);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor pLevel,
                                                  @NotNull DifficultyInstance pDifficulty,
                                                  @NotNull MobSpawnType pReason,
                                                  @Nullable SpawnGroupData pSpawnData,
                                                  @Nullable CompoundTag pDataTag) {
        if (pSpawnData == null) {
            this.setVariant(generateVariant());
        }
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("variant", entityData.get(VARIANT));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("variant")) {
            this.setVariant(tag.getInt("variant"));
        } else {
            this.setVariant(Variant.DEFAULT);
        }
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return ModSoundEvents.IMPALER_DEATH.get();
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) {
        return ModSoundEvents.IMPALER_HURT.get();
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSoundEvents.IMPALER_IDLE.get();
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pPos, BlockState pState) {
        this.playSound(pState.getSoundType().getStepSound(), this.isSprinting() ? .75f : .15f, 1.0F);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
