package com.jeremyseq.inhabitants.entities.boulder;

import com.jeremyseq.inhabitants.entities.goals.AnimatedCooldownMeleeAttackGoal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

public class BoulderEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public BoulderEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier setAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20f)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.25f)
                .add(Attributes.KNOCKBACK_RESISTANCE, .25f)
                .add(Attributes.MOVEMENT_SPEED, 0.2F).build();
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player) {
            player.getMainHandItem();
            var item = player.getMainHandItem().getItem();

            if (item instanceof PickaxeItem) {
                amount *= 2.5f;
            }
        }

        return super.hurt(source, amount);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new BoulderAreaAttackGoal(this, 10, 140, 32, 15));
        this.goalSelector.addGoal(2, new AnimatedCooldownMeleeAttackGoal(this, 1,
                true, 31, "attack", "melee",
                31, 19).setFreezeMovement(12, 23));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D, 0.0F));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 3, this::predicate));
        controllers.add(new AnimationController<>(this, "attack", 3, state -> PlayState.STOP)
                .triggerableAnim("melee", RawAnimation.begin().then("attack_punch", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("area", RawAnimation.begin().then("attack_area", Animation.LoopType.PLAY_ONCE)));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> animationState) {
        animationState.getController().setAnimation(RawAnimation.begin().then("idle_movement", Animation.LoopType.LOOP));

        return PlayState.CONTINUE;
    }

    @Override
    public double getMeleeAttackRangeSqr(@NotNull LivingEntity pEntity) {
        return super.getMeleeAttackRangeSqr(pEntity) + 8;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
