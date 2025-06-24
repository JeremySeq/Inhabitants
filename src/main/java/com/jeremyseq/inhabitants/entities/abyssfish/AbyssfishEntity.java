package com.jeremyseq.inhabitants.entities.abyssfish;

import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class AbyssfishEntity extends AbstractFish implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("swimming");
    private static final RawAnimation FLOP_ANIM = RawAnimation.begin().thenLoop("flopping");

    public AbyssfishEntity(EntityType<? extends AbstractFish> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier setAttributes() {
        return AbstractFish.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 3f)
                .add(Attributes.MOVEMENT_SPEED, 1.0D).build();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25D));
        this.goalSelector.addGoal(2, new RandomSwimmingGoal(this, 1.0D, 40));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.isInWater() && this.onGround() && this.verticalCollision) {
            this.setDeltaMovement(
                    this.getDeltaMovement().add(
                            (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F,
                            0.4F,
                            (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F
                    )
            );
            this.setOnGround(false);
            this.hasImpulse = true;
            this.hurt(this.damageSources().dryOut(), 1.0F);
        }
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.RAW_ABYSSFISH.get()));
    }

    @Override
    protected @NotNull SoundEvent getFlopSound() {
        return SoundEvents.COD_FLOP;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> animationState) {
        if (this.isInWater()) {
            animationState.setAnimation(SWIM_ANIM);
        } else {
            animationState.setAnimation(FLOP_ANIM);
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public @NotNull ItemStack getBucketItemStack() {
        return new ItemStack(Items.COD_BUCKET);
    }
}
