package com.jeremyseq.inhabitants.entities.bogre.recipe;

import com.jeremyseq.inhabitants.entities.EntityUtil;
import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron.BogreCauldronEntity;
import com.jeremyseq.inhabitants.entities.bogre.skill.BogreSkills;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Block;

import java.util.*;

/**
 * 
 */
public final class BogreCraftingManager {

    private BogreCraftingManager() {}

    public static void aiStep(BogreEntity bogre) {
        BogreRecipe activeRecipe = bogre.getActiveRecipe();
        if (activeRecipe == null) {
            bogre.setAIState(BogreEntity.State.CAUTIOUS);
            return;
        }
        BogreSkills.forType(activeRecipe.type()).aiStep(bogre);
    }

    //methods shared across skills

    public static boolean handleThrowingResult(BogreEntity bogre) {
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
                    bogre.setCraftingState(BogreEntity.CraftingState.NONE);
                    bogre.setActiveRecipe(null);
                    bogre.setDroppedIngredientPlayer(null);
                    bogre.setChowderThrowDelay(-1);
                    bogre.setPathSet(false);
                    bogre.resetStuckTicks();
                    return true;
                }
            } else {
                throwHeldItem(bogre);
                bogre.setAIState(BogreEntity.State.CAUTIOUS);
                bogre.setCraftingState(BogreEntity.CraftingState.NONE);
                bogre.setActiveRecipe(null);
                bogre.setChowderThrowDelay(-1);
                bogre.setPathSet(false);
                bogre.resetStuckTicks();
                return true;
            }
        }
        return false;
    }

    public static void throwHeldItem(BogreEntity bogre) {
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