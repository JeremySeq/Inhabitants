package com.jeremyseq.inhabitants.entities.bogre.recipe;

import com.jeremyseq.inhabitants.entities.EntityUtil;
import com.jeremyseq.inhabitants.entities.PrecisePathNavigation;
import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron.BogreCauldronEntity;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * 
 */

public class BogreCraftingManager {

    public static void carveBoneAiStep(BogreEntity bogre) {
        BogreRecipe activeRecipe = bogre.getActiveRecipe();
        if (activeRecipe == null) {
            bogre.setAIState(BogreEntity.State.CAUTIOUS);
            return;
        }

        if (handleThrowingResult(bogre)) {
            return;
        }

        List<BlockPos> carvePositions = bogre.getCarvePositions();
        if (carvePositions == null || carvePositions.isEmpty()) {
            bogre.setAIState(BogreEntity.State.CAUTIOUS);
            bogre.setActiveRecipe(null);
            return;
        }

        BlockPos center = getAveragePosition(carvePositions);
        double distance = bogre.distanceToSqr(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        if (Math.sqrt(distance) > 2.5) {
            bogre.moveTo(center, 1);
            bogre.resetCarveTicks();
            bogre.getEntityData().set(BogreEntity.CARVING_ANIM, false);
            return;
        }

        bogre.getNavigation().stop();
        
        Vec3 boneTarget = Vec3.atCenterOf(center);
        bogre.getLookControl().setLookAt(boneTarget.x, boneTarget.y + 0.5, boneTarget.z, 100f, 100f);

        if (bogre.getCarveTicks() == 0) {
            bogre.getEntityData().set(BogreEntity.CARVING_ANIM, true);
        }

        // validate blocks are still present
        if (activeRecipe.triggerBlock().isPresent()) {
            Block requiredBlock = activeRecipe.triggerBlock().get();
            for (BlockPos pos : carvePositions) {
                if (!bogre.level().getBlockState(pos).is(requiredBlock)) {
                    bogre.setAIState(BogreEntity.State.CAUTIOUS);
                    bogre.setActiveRecipe(null);
                    bogre.getEntityData().set(BogreEntity.CARVING_ANIM, false);
                    bogre.resetCarveTicks();
                    return;
                }
            }
        }
        
        bogre.incrementCarveTicks();

        int timeTicks = activeRecipe.timeTicks();

        if (bogre.getCarveTicks() == timeTicks) {
            bogre.setItemHeld(activeRecipe.result().copy());
            bogre.setChowderThrowDelay(20);
            
            for (BlockPos pos : carvePositions) {
                bogre.level().destroyBlock(pos, false);
            }
            bogre.playSound(SoundEvents.STONE_BREAK, 1.0F, 0.7F);
            
            bogre.getEntityData().set(BogreEntity.CARVING_ANIM, false);
            bogre.resetCarveTicks();
        }
    }

    public static void carveDiscAiStep(BogreEntity bogre) {
        BogreRecipe activeRecipe = bogre.getActiveRecipe();
        if (activeRecipe == null) {
            bogre.setAIState(BogreEntity.State.CAUTIOUS);
            return;
        }

        if (handleThrowingResult(bogre)) {
            return;
        }

        List<BlockPos> carvePositions = bogre.getCarvePositions();
        if (carvePositions == null || carvePositions.isEmpty()) {
            bogre.setAIState(BogreEntity.State.CAUTIOUS);
            bogre.setActiveRecipe(null);
            return;
        }

        BlockPos center = carvePositions.get(0);
        ItemEntity nearestBrokenDisc = findBrokenDisc(bogre, (int) BogreEntity.ROAR_RANGE);

        if (nearestBrokenDisc != null) {
            double distance = bogre.distanceTo(nearestBrokenDisc);
            if (distance > 3.5) {
                Vec3 discPos = nearestBrokenDisc.position();
                Vec3 dir = bogre.position().subtract(discPos).normalize();
                Vec3 approachPos = discPos.add(dir.scale(2.5));
                PrecisePathNavigation preciseNav = (PrecisePathNavigation) bogre.getNavigation();
                preciseNav.preciseMoveTo(approachPos, 1.0D);
                bogre.resetCarveTicks();
                bogre.getEntityData().set(BogreEntity.CARVING_ANIM, false);
                return;
            }
        }

        bogre.getNavigation().stop();
        
        if (nearestBrokenDisc != null) {
            bogre.getLookControl().setLookAt(nearestBrokenDisc.getX(), nearestBrokenDisc.getY(), nearestBrokenDisc.getZ(), 100f, 100f);
        } else {
            Vec3 discCenter = Vec3.atCenterOf(center);
            bogre.getLookControl().setLookAt(discCenter.x, discCenter.y + 0.5, discCenter.z, 100f, 100f);
        }

        if (bogre.getCarveTicks() == 0) {
            bogre.getEntityData().set(BogreEntity.CARVING_ANIM, true);
        }

        // validate disc is still present and valid
        if (nearestBrokenDisc == null || !nearestBrokenDisc.isAlive()) {
            bogre.setAIState(BogreEntity.State.CAUTIOUS);
            bogre.setActiveRecipe(null);
            bogre.getEntityData().set(BogreEntity.CARVING_ANIM, false);
            bogre.resetCarveTicks();
            return;
        }
        
        bogre.incrementCarveTicks();

        int timeTicks = activeRecipe.timeTicks();

        if (bogre.getCarveTicks() == timeTicks) {
            if (nearestBrokenDisc.isAlive()) {
                nearestBrokenDisc.discard();
                bogre.playSound(SoundEvents.STONE_BREAK, 1.0F, 0.7F);
            }
            
            if (activeRecipe.result() != null && !activeRecipe.result().isEmpty()) {
                ItemEntity resultItem = new ItemEntity(bogre.level(), center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5, activeRecipe.result().copy());
                resultItem.setDefaultPickUpDelay();
                bogre.level().addFreshEntity(resultItem);
            }
            
            bogre.getNavigation().stop();
            bogre.setAIState(BogreEntity.State.CAUTIOUS);
            bogre.setActiveRecipe(null);
            bogre.setCarvePositions(List.of());
            bogre.getEntityData().set(BogreEntity.CARVING_ANIM, false);
            bogre.resetCarveTicks();
        }
    }

    public static void makeChowderAiStep(BogreEntity bogre) {
        BogreRecipe activeRecipe = bogre.getActiveRecipe();
        if (activeRecipe == null) {
            bogre.setAIState(BogreEntity.State.CAUTIOUS);
            return;
        }

        if (handleThrowingResult(bogre)) {
            return;
        }

        if (!bogre.getItemHeld().isEmpty()) {
            BogreCauldronEntity bogreCauldron = getCauldronEntity(bogre);
            if (bogreCauldron == null) {
                bogre.setAIState(BogreEntity.State.CAUTIOUS);
                bogre.setActiveRecipe(null);
                return;
            }
            Direction direction = bogreCauldron.getDirection();
            Direction dirLeft = direction.getCounterClockWise(Direction.Axis.Y);

            final float forwardDist = 2.25f;
            final float leftDist = .9f;

            Vec3i forwardI = direction.getNormal();
            Vec3i leftI = dirLeft.getNormal();
            Vec3 forward = new Vec3(forwardI.getX(), forwardI.getY(), forwardI.getZ()).scale(forwardDist);
            Vec3 left = new Vec3(leftI.getX(), leftI.getY(), leftI.getZ()).scale(leftDist);

            Vec3 targetCenter = Vec3.atBottomCenterOf(bogre.cauldronPos).add(forward).add(left);

            double distSqr = bogre.distanceToSqr(targetCenter);
            PrecisePathNavigation preciseNav = (PrecisePathNavigation) bogre.getNavigation();
            Vec3 currentPos = bogre.position();

            if (bogre.getLastPos() != null) {
                double movedSq = currentPos.distanceToSqr(bogre.getLastPos());
                if (movedSq < 0.0025 && distSqr > 0.3) {
                    bogre.incrementStuckTicks();
                } else {
                    bogre.resetStuckTicks();
                }
            }

            bogre.setLastPos(currentPos);

            if (bogre.getStuckTicks() > 40 && bogre.entrancePos != null) {
                Vec3 tp = Vec3.atCenterOf(bogre.entrancePos);
                bogre.setPos(tp.x, tp.y, tp.z);
                bogre.getNavigation().stop();
                bogre.resetStuckTicks();
                bogre.setPathSet(false);
                return;
            }

            if (!bogre.isPathSet() && distSqr > .3) {
                preciseNav.preciseMoveTo(targetCenter, 1.0D);
                bogre.setPathSet(true);
                return;
            }

            if (distSqr > .3) return;

            bogre.setPathSet(false);
            bogre.getNavigation().stop();

            bogre.lookAt(EntityAnchorArgument.Anchor.FEET, bogre.cauldronPos.getCenter());
            bogre.lookAt(EntityAnchorArgument.Anchor.EYES, bogre.cauldronPos.getCenter());
            
            if (bogre.getCookingTicks() == BogreEntity.DROP_FISH_OFFSET) {
                bogre.triggerAnim("grab", "grab");
            } else if (bogre.getCookingTicks() == 25) {
                bogre.getEntityData().set(BogreEntity.COOKING_ANIM, false);
                bogre.getEntityData().set(BogreEntity.COOKING_ANIM, true);
                bogreCauldron.setCooking(true);
            }

            bogre.incrementCookingTicks();

            int cookingTime = activeRecipe.timeTicks();

            if (bogre.getCookingTicks() >= cookingTime) {
                bogre.playSound(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 1.0F, 0.8F);
                bogre.getTamedPlayers().add(bogre.getDroppedIngredientPlayer().getUUID());
                bogreCauldron.setCooking(false);

                float stewChance = activeRecipe.suspiciousStewChance();

                if (bogre.level().random.nextFloat() < stewChance) {
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

                    MobEffectInstance chosen = effects[bogre.level().random.nextInt(effects.length)];
                    SuspiciousStewItem.saveMobEffect(stew, chosen.getEffect(), chosen.getDuration());
                    bogre.setItemHeld(stew);
                } else {
                    bogre.setItemHeld(activeRecipe.result().copy());
                }
                bogre.setChowderThrowDelay(20);
                bogre.resetCookingTicks();
            }
            return;
        }

        ItemEntity droppedIngredientItem = bogre.getDroppedIngredientItem();
        if (droppedIngredientItem == null || !droppedIngredientItem.isAlive()) {
            bogre.setAIState(BogreEntity.State.CAUTIOUS);
            bogre.setActiveRecipe(null);
            return;
        }

        double distance = bogre.distanceTo(droppedIngredientItem);
        if (distance > 3) {
            BlockPos pos = new BlockPos(droppedIngredientItem.getBlockX(), droppedIngredientItem.getBlockY(), droppedIngredientItem.getBlockZ());
            bogre.moveTo(pos, 1, false);
            return;
        }

        if (bogre.getItemHeld().isEmpty()) {
            bogre.triggerAnim("grab", "grab");
            bogre.getNavigation().stop();
            ItemStack ingredientStack = droppedIngredientItem.getItem();
            
            if (ingredientStack.getCount() > 1) {
                ingredientStack.shrink(1);
            } else {
                droppedIngredientItem.discard();
            }

            bogre.setItemHeld(new ItemStack(ingredientStack.getItem(), 1));
            bogre.setDroppedIngredientItem(null);
        }
    }

    public static ItemEntity findBrokenDisc(BogreEntity bogre, int range) {
        BlockPos origin = bogre.blockPosition();
        AABB searchBox = new AABB(origin).inflate(range);
        List<ItemEntity> discs = bogre.level().getEntitiesOfClass(
                ItemEntity.class,
                searchBox,
                item -> item.isAlive() && BogreRecipeManager.isTransformationIngredient(item.getItem().getItem())
        );
        if (discs.isEmpty()) return null;
        return discs.get(0);
    }

    public static List<BlockPos> findCarvableBlocks(BogreEntity bogre, int radius) {
        BlockPos origin = bogre.blockPosition();
        List<BlockPos> carvableBlocks = new ArrayList<>();

        BlockPos.betweenClosedStream(origin.offset(-radius, -3, -radius), origin.offset(radius, 3, radius))
                .forEach(pos -> {
                    if (BogreRecipeManager.isCarvable(bogre.level().getBlockState(pos).getBlock())) {
                        carvableBlocks.add(pos.immutable());
                    }
                });

        carvableBlocks.sort(Comparator.comparingDouble(pos -> pos.distSqr(origin)));
        Set<BlockPos> blockSet = new HashSet<>(carvableBlocks);

        for (BlockPos pos : carvableBlocks) {
            Block block = bogre.level().getBlockState(pos).getBlock();
            Optional<BogreRecipe> recipeOpt = BogreRecipeManager.getCarvingRecipe(block);
            if (recipeOpt.isEmpty()) continue;
            
            int required = recipeOpt.get().requiredBlocks();
            if (required <= 1) {
                return List.of(pos);
            }

            // try X direction line
            List<BlockPos> xLine = new ArrayList<>();
            for (int i = 0; i < required; i++) {
                BlockPos nextPos = pos.offset(i, 0, 0);
                if (blockSet.contains(nextPos) && bogre.level().getBlockState(nextPos).is(block)) {
                    xLine.add(nextPos);
                } else {
                    break;
                }
            }
            if (xLine.size() == required) return xLine;
            
            List<BlockPos> zLine = new ArrayList<>();
            for (int i = 0; i < required; i++) {
                BlockPos nextPos = pos.offset(0, 0, i);
                if (blockSet.contains(nextPos) && bogre.level().getBlockState(nextPos).is(block)) {
                    zLine.add(nextPos);
                } else {
                    break;
                }
            }
            if (zLine.size() == required) return zLine;
            
            List<BlockPos> yLine = new ArrayList<>();
            for (int i = 0; i < required; i++) {
                BlockPos nextPos = pos.offset(0, i, 0);
                if (blockSet.contains(nextPos) && bogre.level().getBlockState(nextPos).is(block)) {
                    yLine.add(nextPos);
                } else {
                    break;
                }
            }
            if (yLine.size() == required) return yLine;
        }
        return null;
    }

    public static Player findNearbyTrustedPlayer(BogreEntity bogre, BlockPos center, double radius) {
        List<Player> players = bogre.level().getEntitiesOfClass(Player.class, new AABB(center).inflate(radius));
        for (Player player : players) {
            if (bogre.isTamedBy(player) || player.isCreative()) {
                return player;
            }
        }
        return null;
    }

    private static boolean handleThrowingResult(BogreEntity bogre) {
        if (!bogre.getItemHeld().isEmpty() && bogre.getChowderThrowDelay() != -1) {
            Player droppedIngredientPlayer = bogre.getDroppedIngredientPlayer();
            if (droppedIngredientPlayer != null) {
                if (bogre.getChowderThrowDelay() > 0) {
                    bogre.lookAt(EntityAnchorArgument.Anchor.FEET, droppedIngredientPlayer.position());
                    bogre.decrementChowderThrowDelay();
                    return true;
                } else if (bogre.getChowderThrowDelay() == 0) {
                    throwHeldItem(bogre);
                    bogre.setAIState(BogreEntity.State.CAUTIOUS);
                    bogre.setActiveRecipe(null);
                    bogre.setDroppedIngredientPlayer(null);
                    bogre.setChowderThrowDelay(-1);
                    return true;
                }
            } else {
                throwHeldItem(bogre);
                bogre.setAIState(BogreEntity.State.CAUTIOUS);
                bogre.setActiveRecipe(null);
                bogre.setChowderThrowDelay(-1);
                return true;
            }
        }
        return false;
    }

    private static void throwHeldItem(BogreEntity bogre) {
        bogre.triggerAnim("grab", "grab");
        EntityUtil.throwItemStack(bogre.level(), bogre, bogre.getItemHeld(), .3f, 0);
        bogre.setItemHeld(ItemStack.EMPTY);
    }

    public static BogreCauldronEntity getCauldronEntity(BogreEntity bogre) {
        if (bogre.cauldronPos == null) return null;
        List<BogreCauldronEntity> entities = bogre.level().getEntitiesOfClass(
                BogreCauldronEntity.class,
                new AABB(bogre.cauldronPos),
                entity -> !entity.isRemoved()
        );
        return entities.isEmpty() ? null : entities.get(0);
    }

    public static BlockPos getAveragePosition(List<BlockPos> positions) {
        int x = 0, y = 0, z = 0;
        for (BlockPos pos : positions) {
            x += pos.getX();
            y += pos.getY();
            z += pos.getZ();
        }
        return new BlockPos(x / positions.size(), y / positions.size(), z / positions.size());
    }
}