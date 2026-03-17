package com.jeremyseq.inhabitants.entities.bogre.ai;

import com.jeremyseq.inhabitants.debug.DevMode;
import com.jeremyseq.inhabitants.blocks.ModBlocks;
import com.jeremyseq.inhabitants.debug.BogreDebugRenderer;
import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogreAi;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;

/**
 * Path navigation for Bogre
 * - Simplified for smoother movement while retaining precise final arrival
 * - Added support for invisible cauldron blocks
 * - Added support for chain blocks
 * - Added support for Carving to target nearest side
 * 
 * need to fix:
 * - avoiding fire and lava
 */
public class BogrePathNavigation extends GroundPathNavigation {

    private Vec3 preciseTarget = null;
    private static final double TOLERANCE_SQ = 0.1D * 0.1D;

    public BogrePathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int pRange) {
        this.nodeEvaluator = new WalkNodeEvaluator() {
            @Override
            public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z) {
                BlockPathTypes type = super.getBlockPathType(level, x, y, z);
                
                BlockPos pos = new BlockPos(x, y, z);
                
                if (level.getBlockState(pos).is(Blocks.CHAIN)) {
                    for (int i = 1; i <= 4; i++) {
                        if (level.getBlockState(pos.below(i)).is(ModBlocks.INVISIBLE_CAULDRON_BLOCK.get())) {
                            return BlockPathTypes.OPEN;
                        }
                    }
                }
                
                for (int dx = -1; dx <= 0; dx++) {
                    for (int dz = -1; dz <= 0; dz++) {
                        if (level.getBlockState(pos.offset(dx, 0, dz)).is(ModBlocks.INVISIBLE_CAULDRON_BLOCK.get())) {
                            return BlockPathTypes.BLOCKED;
                        }
                    }
                }
                
                return type;
            }
        };
        
        return new PathFinder(this.nodeEvaluator, pRange) {
            @Override
            protected float distance(Node pNode1, Node pNode2) {
                return pNode1.distanceToXZ(pNode2);
            }
        };
    }
    
    public boolean preciseMoveTo(Vec3 target, double speed) {
        double distSq = this.mob.position().distanceToSqr(target.x, this.mob.getY(), target.z);
        double yDist = Math.abs(this.mob.getY() - target.y);
        
        if (distSq < TOLERANCE_SQ && yDist < 1.5D) {
            this.stop();
            return true;
        }
        
        if (this.preciseTarget != null &&
        this.preciseTarget.distanceToSqr(target) < 0.01D &&
        !this.isDone()) {
            this.speedModifier = speed;
            return true;
        }

        this.preciseTarget = target;
        return super.moveTo(target.x, target.y, target.z, speed);
    }

    @Override
    public void stop() {
        super.stop();
        this.preciseTarget = null;
    }

    @Override
    public void tick() {
        super.tick();
        
        if (DevMode.bogrePathfinding() &&
        this.level instanceof ServerLevel serverLevel &&
        this.tick % 5 == 0) {
            BogreDebugRenderer.renderPath(serverLevel, this.path, this.preciseTarget, this.mob.position());
        }

        if (this.isDone() && this.preciseTarget != null) {
            handlePreciseArrival();
        }
    }

    private void handlePreciseArrival() {
        if (this.preciseTarget == null) return;
        
        double distSq = this.mob.position().distanceToSqr(
            this.preciseTarget.x, 
            this.mob.getY(), 
            this.preciseTarget.z
        );

        double yDist = Math.abs(this.mob.getY() - this.preciseTarget.y);

        if (distSq > TOLERANCE_SQ || yDist > 1.5D) {
            this.mob.getMoveControl().setWantedPosition(
                preciseTarget.x, 
                preciseTarget.y, 
                preciseTarget.z, 
                this.speedModifier
            );
        } else {
            this.preciseTarget = null;
        }
    }
    
    public boolean moveToCarvingTarget(Vec3 boneTarget, float minDistance, float maxDistance) {
        if (!(this.mob instanceof BogreEntity bogre)) return false;
        
        float targetDist = (minDistance + maxDistance) / 2.0f;
        BogreAi ai = bogre.getAi();
        Vec3 moveTarget;
        int lockedSide = ai.getSkillingMoveSide();
        
        if (ai.getSkillingMoveSide() != 99 && bogre.getAiTicks() > 100) {
            ai.setSkillingMoveSide(99);
        }

        if (ai.getSkillingMoveSide() == 0) {
            List<SideChoice> choices = new ArrayList<>();

            choices.add(new SideChoice(1,
            new Vec3(boneTarget.x + targetDist, boneTarget.y, boneTarget.z), 
            bogre.position().distanceToSqr(boneTarget.x + targetDist, bogre.getY(), boneTarget.z)));

            choices.add(new SideChoice(-1,
            new Vec3(boneTarget.x - targetDist, boneTarget.y, boneTarget.z), 
            bogre.position().distanceToSqr(boneTarget.x - targetDist, bogre.getY(), boneTarget.z)));

            choices.add(new SideChoice(2,
            new Vec3(boneTarget.x, boneTarget.y, boneTarget.z + targetDist), 
            bogre.position().distanceToSqr(boneTarget.x, bogre.getY(), boneTarget.z + targetDist)));

            choices.add(new SideChoice(-2,
            new Vec3(boneTarget.x, boneTarget.y, boneTarget.z - targetDist), 
            bogre.position().distanceToSqr(boneTarget.x, bogre.getY(), boneTarget.z - targetDist)));

            choices.sort((c1, c2) -> Double.compare(c1.distSq, c2.distSq));

            for (SideChoice choice : choices) {
                if (!isPositionBlocked(bogre, choice.pos.x, choice.pos.y, choice.pos.z)) {
                    Path path = this.createPath(BlockPos.containing(choice.pos), 0);
                    if (path != null && path.canReach()) {
                        ai.setSkillingMoveSide(choice.side);
                        break;
                    }
                }
            }
            
            if (ai.getSkillingMoveSide() == 0) {
                ai.setSkillingMoveSide(99); 
            }
        }
        
        if (lockedSide == 99) {
            double dx = bogre.getX() - boneTarget.x;
            double dz = bogre.getZ() - boneTarget.z;

            Vec3 dir = (dx * dx + dz * dz < 0.01) ?
            bogre.getForward() : new Vec3(dx, 0, dz).normalize();

            moveTarget = new Vec3(
                boneTarget.x + dir.x * targetDist, 
                boneTarget.y, 
                boneTarget.z + dir.z * targetDist
            );
        } else if (Math.abs(lockedSide) == 1) {
            moveTarget = new Vec3(
                boneTarget.x + (lockedSide * targetDist), 
                boneTarget.y, 
                boneTarget.z
            );
        } else {
            moveTarget = new Vec3(
                boneTarget.x, 
                boneTarget.y, 
                boneTarget.z + ((lockedSide / 2.0) * targetDist)
            );
        }

        double distToSpotSq = bogre.position().distanceToSqr(moveTarget.x, bogre.getY(), moveTarget.z);
        double yDist = Math.abs(bogre.getY() - moveTarget.y);

        if (distToSpotSq > 0.3 * 0.3 || yDist > 1.5D) {
            this.preciseMoveTo(moveTarget, 1.0D);
            bogre.incrementAiTicks();
            return false;
        } else {
            this.stop();
            return true;
        }
    }

    private boolean isPositionBlocked(BogreEntity bogre, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        return isSolid(bogre, pos) || isSolid(bogre, pos.above());
    }

    private boolean isSolid(BogreEntity bogre, BlockPos pos) {
        BlockState state = bogre.level().getBlockState(pos);
        if (state.isAir()) return false;
        return !state.getCollisionShape(bogre.level(), pos).isEmpty();
    }

    private class SideChoice {
        final int side;
        final Vec3 pos;
        final double distSq;

        SideChoice(int side, Vec3 pos, double dSq) {
            this.side = side;
            this.pos = pos;
            this.distSq = dSq;
        }
    }
}
