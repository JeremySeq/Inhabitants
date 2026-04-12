package com.jeremyseq.inhabitants.entities.bogre.utilities;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron.BogreCauldronEntity;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogreAi;
import com.jeremyseq.inhabitants.recipe.CarvingRecipe;
import com.jeremyseq.inhabitants.recipe.BogreRecipeManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;

import java.util.*;
import java.util.function.Predicate;

public class BogreDetectionHelper {
    private static final int JUKEBOX_RANGE = 15;

    public static boolean isJukeboxPlayingNearby(BogreEntity bogre) {
        BlockPos origin = bogre.blockPosition();
        return BlockPos.betweenClosedStream(
            origin.offset(-JUKEBOX_RANGE, -5, -JUKEBOX_RANGE),
            origin.offset(JUKEBOX_RANGE, 5, JUKEBOX_RANGE)).anyMatch(pos -> {
                BlockEntity blockEntity = bogre.level().getBlockEntity(pos);
                return blockEntity instanceof JukeboxBlockEntity jukeboxblockentity
                    && jukeboxblockentity.isRecordPlaying();
            });
    }

    public static boolean isValidCauldron(Level level, BlockPos pos) {
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY();
        double centerZ = pos.getZ() + 0.5;

        List<BogreCauldronEntity> entities = level.getEntitiesOfClass(
            BogreCauldronEntity.class,
            new AABB(centerX - 0.5, centerY - 1, centerZ - 0.5, centerX + 0.5, centerY + 2, centerZ + 0.5));

        for (BogreCauldronEntity entity : entities) {
            if (entity.blockPosition().equals(pos)) {
                return true;
            }
        }
        return false;
    }

    public static boolean canSeeCauldron(BogreEntity bogre) {
        if (bogre.cauldronPos == null) return false;
        Vec3 eyePos = bogre.getEyePosition();
        Vec3 target = Vec3.atCenterOf(bogre.cauldronPos);

        HitResult hit = bogre.level().clip(new ClipContext(
            eyePos,
            target,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            bogre));

        if (hit.getType() == HitResult.Type.MISS) {
            return true;
        }

        if (hit instanceof BlockHitResult bhr) {
            return bhr.getBlockPos().equals(bogre.cauldronPos);
        }

        return false;
    }

    public static Optional<BlockPos> findNearestCauldron(BogreEntity bogre, int radius) {
        return BlockPos.betweenClosedStream(bogre.blockPosition().offset(-radius, -2, -radius),
            bogre.blockPosition().offset(radius, 2, radius))
            .map(BlockPos::immutable)
            .filter(bogre::isValidCauldron)
            .findFirst();
    }


    public static ItemEntity findTransformationItem(BogreEntity bogre, int range) {
        BlockPos origin = bogre.blockPosition();
        AABB searchBox = new AABB(origin).inflate(range);
        List<ItemEntity> items = bogre.level().getEntitiesOfClass(
            ItemEntity.class,
            searchBox,
            item -> item.isAlive() && BogreRecipeManager.isTransformationIngredient(item.getItem().getItem())
        );
        if (items.isEmpty()) return null;
        return items.get(0);
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

        // Use stable coordinate sorting to ensure the "middle" block doesn't jump as Bogre moves
        carvableBlocks.sort((p1, p2) -> {
            if (p1.getX() != p2.getX()) return p1.getX() - p2.getX();
            if (p1.getY() != p2.getY()) return p1.getY() - p2.getY();
            return p1.getZ() - p2.getZ();
        });
        Set<BlockPos> blockSet = new HashSet<>(carvableBlocks);

        for (BlockPos pos : carvableBlocks) {
            Block block = bogre.level().getBlockState(pos).getBlock();
            Optional<CarvingRecipe> recipeOpt = BogreRecipeManager.getCarvingRecipe(block);
            if (recipeOpt.isEmpty()) continue;
            
            int required = recipeOpt.get().requiredBlocks();
            if (required <= 1) {
                return List.of(pos);
            }

            // check line directions
            if (checkLine(bogre.level(), pos, block, required, 1, 0, 0, blockSet))
                return offsetLine(pos, required, 1, 0, 0);
            if (checkLine(bogre.level(), pos, block, required, 0, 0, 1, blockSet))
                return offsetLine(pos, required, 0, 0, 1);
        }
        return Collections.emptyList();
    }

    // check if a line of blocks is valid
    private static boolean checkLine(Level level, BlockPos start, Block block,
    int length, int stepX, int stepY, int stepZ, Set<BlockPos> blockSet) {

        for (int i = 0; i < length; i++) {
            BlockPos nextPos = start.offset(i * stepX, i * stepY, i * stepZ);
            if (!blockSet.contains(nextPos) || !level.getBlockState(nextPos).is(block)) {
                return false;
            }
        }
        return true;
    }

    // get a line of blocks
    private static List<BlockPos> offsetLine(BlockPos start, int length,
    int stepX, int stepY, int stepZ) {

        List<BlockPos> line = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            line.add(start.offset(i * stepX, i * stepY, i * stepZ).immutable());
        }
        return line;
    }

    public static Optional<BlockPos> findChestWithSpace(BogreEntity bogre, int radius, int maxToSearch) {
        BlockPos origin = bogre.blockPosition();
        ItemStack held = bogre.getItemHeld();
        if (held.isEmpty()) return Optional.empty();

        return BlockPos.betweenClosedStream(origin.offset(-radius, -3, -radius),
            origin.offset(radius, 3, radius))
            .map(BlockPos::immutable)
            .filter(pos -> {
                
                BlockEntity be = bogre.level().getBlockEntity(pos);
                if (be == null) return false;
                BlockState state = bogre.level().getBlockState(pos);
                if (!(state.getBlock() instanceof ChestBlock)) return false;
                
                if (pos.equals(bogre.getEntityData().get(BogreEntity.TARGET_POS)) && 
                bogre.getAi().getDeliveryState() == BogreAi.DeliveryState.SEARCHING &&
                bogre.getCookingTicks() > 0) {
                    return false;
                }

                return hasAccessibleEmptyChestSlot(bogre.level(), pos, state);
            })
            .sorted(Comparator.comparingDouble(origin::distSqr))
            .findFirst();
    }

    public static boolean hasAccessibleEmptyChestSlot(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof ChestBlock chestBlock)) return false;
        Container container = ChestBlock.getContainer(chestBlock, state, level, pos, true);
        if (container == null) return false;

        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static Optional<BogreEntity> findClosestBogre(Level level, Vec3 pos, double range, Predicate<BogreEntity> filter) {
        List<BogreEntity> nearbyBogres = level.getEntitiesOfClass(
                BogreEntity.class,
                new AABB(pos, pos).inflate(range),
                bogre -> bogre.isAlive() && filter.test(bogre)
        );

        BogreEntity closest = null;
        double minDistSq = Double.MAX_VALUE;

        for (BogreEntity bogre : nearbyBogres) {
            double distSq = bogre.distanceToSqr(pos);
            if (distSq < minDistSq) {
                closest = bogre;
                minDistSq = distSq;
            }
        }
        
        return Optional.ofNullable(closest);
    }
}