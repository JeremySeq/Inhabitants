package com.jeremyseq.inhabitants.entities.wishfish;

import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.AbstractSchoolingFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class WishfishEntity extends AbstractSchoolingFish implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final double CURSE_CHANCE = 0.03;

    public WishfishEntity(EntityType<? extends AbstractSchoolingFish> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new WishfishFleeGoal(this, 5d));
    }

    public static AttributeSupplier setAttributes() {
        return AbstractFish.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 3f)
                .add(Attributes.MOVEMENT_SPEED, 2f).build();
    }

    @Override
    protected @NotNull SoundEvent getFlopSound() {
        return SoundEvents.COD_FLOP;
    }

    @Override
    public @NotNull ItemStack getBucketItemStack() {
        return new ItemStack(ModItems.WISHFISH_BUCKET.get());
    }

    @Override
    protected @NotNull AABB makeBoundingBox() {
        AABB box = super.makeBoundingBox();

        if (!this.isInWater()) {
            double centerX = (box.minX + box.maxX) / 2.0;
            double centerY = (box.minY + box.maxY) / 2.0;
            double centerZ = (box.minZ + box.maxZ) / 2.0;

            double width = .2;
            double height = box.maxY - box.minY;
            double depth = box.maxZ - box.minZ;

            double halfWidth = height / 2.0;
            double halfHeight = width / 2.0;
            double halfDepth = depth / 2.0;

            return new AABB(
                    centerX - halfWidth, centerY - halfHeight, centerZ - halfDepth,
                    centerX + halfWidth, centerY + halfHeight, centerZ + halfDepth
            );
        }

        return box;
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.WISHFISH.get()));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> animationState) {
        if (this.isSprinting()) {
            animationState.setAnimation(RawAnimation.begin().thenLoop("fleeing"));
        } else {
            animationState.setAnimation(RawAnimation.begin().thenLoop("swimming"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!this.level().isClientSide && !stack.isEmpty()) {
            enchantWithFish(stack);
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    private void enchantWithFish(ItemStack stack) {
        // cursed items cannot be enchanted
        if (isCursed(stack)) return;

        RandomSource random = this.level().random;

        // 3% chance to apply a new curse
        if (random.nextDouble() < CURSE_CHANCE) {
            // choose a random curse
            Enchantment curse = getRandomCurse(stack);
            if (curse != null) {
                EnchantmentHelper.setEnchantments(Map.of(curse, 1), stack);
            }
            // play negative sound effect
            this.level().playSound(null, this.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, this.getSoundSource(), 1.25f, 0.5f);
            return;
        }

        // get current enchantments
        Map<Enchantment, Integer> current = EnchantmentHelper.getEnchantments(stack);

        if (current.isEmpty()) {
            // unenchanted: add a single random enchantment at level 1
            Enchantment enchant = getRandomEnchantment(stack);
            if (enchant != null) {
                EnchantmentHelper.setEnchantments(Map.of(enchant, 1), stack);
                this.level().playSound(null, this.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, this.getSoundSource(), 1.0f, 1f);
            }
        } else {
            // try to upgrade existing enchantments
            List<Enchantment> upgradable = new ArrayList<>();
            for (Enchantment e : current.keySet()) {
                if (!e.isCurse() && current.get(e) < e.getMaxLevel()) {
                    upgradable.add(e);
                }
            }

            if (!upgradable.isEmpty()) {
                // upgrade one random enchantment by 1
                Enchantment e = upgradable.get(random.nextInt(upgradable.size()));
                current.put(e, current.get(e) + 1);
                this.level().playSound(null, this.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, this.getSoundSource(), 1.0f, 1f);
            } else {
                // all maxed: add a new random enchantment
                Enchantment newEnchant = getRandomEnchantment(stack);
                if (newEnchant != null && !current.containsKey(newEnchant)) {
                    current.put(newEnchant, 1);
                    this.level().playSound(null, this.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, this.getSoundSource(), 1.0f, 1f);
                }
            }

            // apply updated enchantments
            EnchantmentHelper.setEnchantments(current, stack);
        }
    }

    private Enchantment getRandomEnchantment(ItemStack stack) {
        List<Enchantment> possible = new ArrayList<>();
        for (Enchantment e : ForgeRegistries.ENCHANTMENTS.getValues()) {
            if (!e.isCurse() && e.canEnchant(stack)) {
                possible.add(e);
            }
        }
        if (possible.isEmpty()) return null;

        Random random = new Random();
        return possible.get(random.nextInt(possible.size()));
    }

    private Enchantment getRandomCurse(ItemStack stack) {
        List<Enchantment> possible = new ArrayList<>();
        for (Enchantment e : ForgeRegistries.ENCHANTMENTS.getValues()) {
            if (e.isCurse() && e.canEnchant(stack)) {
                possible.add(e);
            }
        }
        if (possible.isEmpty()) return null;

        Random random = new Random();
        return possible.get(random.nextInt(possible.size()));
    }

    private boolean isCursed(ItemStack stack) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        for (Enchantment e : enchantments.keySet()) {
            if (e.isCurse()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
