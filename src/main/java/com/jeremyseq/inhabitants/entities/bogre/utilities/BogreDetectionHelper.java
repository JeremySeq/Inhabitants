package com.jeremyseq.inhabitants.entities.bogre.utilities;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron.BogreCauldronEntity;
import com.jeremyseq.inhabitants.entities.bogre.recipe.BogreRecipe;
import com.jeremyseq.inhabitants.entities.bogre.recipe.BogreRecipeManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class BogreDetectionHelper {
    private static final int JUKEBOX_RANGE = 15;

    public static boolean isJukeboxPlayingNearby(BogreEntity bogre) {
        BlockPos origin = bogre.blockPosition();
        return BlockPos.betweenClosedStream(
                origin.offset(-JUKEBOX_RANGE, -2, -JUKEBOX_RANGE),
                origin.offset(JUKEBOX_RANGE, 2, JUKEBOX_RANGE)).anyMatch(pos -> {
                    BlockState state = bogre.level().getBlockState(pos);
                    BlockEntity blockEntity = bogre.level().getBlockEntity(pos);
                    return state.is(Blocks.JUKEBOX)
                            && blockEntity instanceof JukeboxBlockEntity jukeboxblockentity
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
            Optional<BogreRecipe> recipeOpt = BogreRecipeManager.getCarvingRecipe(block);
            if (recipeOpt.isEmpty()) continue;
            
            int required = recipeOpt.get().requiredBlocks();
            if (required <= 1) {
                return List.of(pos);
            }

            // check line directions
            if (checkLine(bogre.level(), pos, block, required, 1, 0, 0, blockSet)) return offsetLine(pos, required, 1, 0, 0);
            if (checkLine(bogre.level(), pos, block, required, 0, 0, 1, blockSet)) return offsetLine(pos, required, 0, 0, 1);
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

    public static Vec3 getMouthPosition(BogreEntity bogre) {
        Vec3 look = bogre.getLookAngle();
        Vec3 add = new Vec3(look.x, 0, look.z).normalize().scale(2.25);
        return new Vec3(bogre.getX(), bogre.getY() + 1.8, bogre.getZ()).add(add);
    }
}