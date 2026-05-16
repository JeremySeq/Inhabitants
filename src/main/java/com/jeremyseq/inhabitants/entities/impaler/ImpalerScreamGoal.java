package com.jeremyseq.inhabitants.entities.impaler;

import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

import static net.minecraft.world.level.block.PointedDripstoneBlock.THICKNESS;
import static net.minecraft.world.level.block.PointedDripstoneBlock.TIP_DIRECTION;

public class ImpalerScreamGoal extends Goal {
    private final ImpalerEntity mob;
    private int timer = 0;

    private static final int DURATION = 20;

    private static final int COOLDOWN_TICKS = 300;
    private long lastUsedTick = 0;

    private static final double dripRadius = 15.0d;
    private static final double dripHeight = 20.0d;
    private static final int maxDripDrops = 20;

    public ImpalerScreamGoal(ImpalerEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return mob.getTarget() != null && mob.level().getGameTime() - lastUsedTick >= COOLDOWN_TICKS
                && mob.getTarget().distanceToSqr(mob) <= 36 && mob.getTarget().distanceToSqr(mob) >= 16;
    }

    @Override
    public boolean canContinueToUse() {
        return timer <= DURATION;
    }

    @Override
    public void start() {
        timer = 0;
        if (mob.getTarget() != null) {
            mob.lookAt(EntityAnchorArgument.Anchor.FEET, mob.getTarget().getPosition(0));
        }
        mob.triggerAnim("scream", "scream");
        mob.playSound(ModSoundEvents.IMPALER_SCREAM.get(), 1.0F, 1.0F);
    }

    @Override
    public void stop() {
        lastUsedTick = mob.level().getGameTime();
    }

    @Override
    public void tick() {
        timer++;

        // make sure not moving during scream
        mob.getNavigation().stop();
        mob.setDeltaMovement(Vec3.ZERO);

        if (timer == 6) {
            // trigger client stuff
            triggerDripstoneFall();
        }
    }

    // make pointed dripstone attached to ceilings fall
    private void triggerDripstoneFall() {
        int dropped = 0;
        int r = (int) Math.ceil(dripRadius);
        double radiusSq = dripRadius * dripRadius;

        for (int dx = -r; dx <= r && dropped < maxDripDrops; dx++) {
            for (int dz = -r; dz <= r && dropped < maxDripDrops; dz++) {

                if (dx * dx + dz * dz > radiusSq) continue;

                for (int dy = 1; dy <= dripHeight && dropped < maxDripDrops; dy++) {
                    BlockPos pos = mob.blockPosition().offset(dx, dy, dz);
                    BlockState state = mob.level().getBlockState(pos);

                    if (state.is(Blocks.POINTED_DRIPSTONE) && isStalactite(state)) {
                        BlockState above = mob.level().getBlockState(pos.above());
                        if (!above.isAir() && !above.is(Blocks.POINTED_DRIPSTONE)) {
                            spawnFallingStalactite(state, (ServerLevel) mob.level(), pos);
                            dropped++;
                        }
                    }
                }
            }
        }
    }

    // COPIED FROM `PointedDripstoneBlock`

    private static boolean isPointedDripstoneWithDirection(BlockState pState, Direction pDir) {
        return pState.is(Blocks.POINTED_DRIPSTONE) && pState.getValue(TIP_DIRECTION) == pDir;
    }

    private static boolean isStalactite(BlockState pState) {
        return isPointedDripstoneWithDirection(pState, Direction.DOWN);
    }

    private static boolean isTip(BlockState pState, boolean pIsTipMerge) {
        if (!pState.is(Blocks.POINTED_DRIPSTONE)) {
            return false;
        } else {
            DripstoneThickness dripstonethickness = pState.getValue(THICKNESS);
            return dripstonethickness == DripstoneThickness.TIP || pIsTipMerge && dripstonethickness == DripstoneThickness.TIP_MERGE;
        }
    }

    private static void spawnFallingStalactite(BlockState pState, ServerLevel pLevel, BlockPos pPos) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable();

        for(BlockState blockstate = pState; isStalactite(blockstate); blockstate = pLevel.getBlockState(blockpos$mutableblockpos)) {
            FallingBlockEntity fallingblockentity = FallingBlockEntity.fall(pLevel, blockpos$mutableblockpos, blockstate);
            if (isTip(blockstate, true)) {
                int i = Math.max(1 + pPos.getY() - blockpos$mutableblockpos.getY(), 6);
                float f = (float) i;
                fallingblockentity.setHurtsEntities(f, 40);
                break;
            }

            blockpos$mutableblockpos.move(Direction.DOWN);
        }
    }
}
