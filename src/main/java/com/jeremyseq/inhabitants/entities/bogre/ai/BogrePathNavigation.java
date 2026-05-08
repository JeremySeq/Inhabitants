package com.jeremyseq.inhabitants.entities.bogre.ai;

import com.jeremyseq.inhabitants.debug.DefaultDebugRenderer;
import com.jeremyseq.inhabitants.debug.DevMode;
import com.jeremyseq.inhabitants.blocks.ModBlocks;
import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.tags.BlockTags;

import java.util.*;

/**
 * Path navigation for Bogre
 * - Simplified for smoother movement while retaining precise final arrival
 * - Added support for invisible cauldron blocks
 * - Added support for chain blocks
 * - Added support for Carving to target the nearest side
 */
public class BogrePathNavigation extends GroundPathNavigation {

    private Vec3 preciseTarget = null;
    private static final double TOLERANCE_SQ = 0.1D * 0.1D;
    private static final double STUCK_THRESHOLD_SQ = 0.015D * 0.015D;
    private static final int MAX_REPATH_ATTEMPTS = 3;
    private static final int STUCK_REPATH_INTERVAL = 30;

    private Vec3 lastPos = Vec3.ZERO;
    private int stuckTicks = 0;
    private int preciseArrivalTicks = 0;
    private int repathAttempts = 0;
    private boolean isNearEnough = false;

    public BogrePathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int pRange) {
        this.nodeEvaluator = new WalkNodeEvaluator() {
            @Override
            public void prepare(PathNavigationRegion region, Mob mob) {
                super.prepare(region, mob);
                this.entityWidth = 2;
                this.entityDepth = 2;
            }

            @Override
            public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z) {
                BlockPathTypes type = super.getBlockPathType(level, x, y, z);
                
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(pos);

                //avoid fire, lava, magma block
                if (state.is(Blocks.FIRE) || state.is(Blocks.LAVA) ||
                        state.is(Blocks.MAGMA_BLOCK)) {
                    return BlockPathTypes.DAMAGE_FIRE;
                }


                // avoid chain blocks above cauldron blocks
                if (level.getBlockState(pos).is(Blocks.CHAIN)) {
                    for (int i = 1; i <= 4; i++) {
                        if (level.getBlockState(pos.below(i)).is(ModBlocks.INVISIBLE_CAULDRON_BLOCK.get())) {
                            return BlockPathTypes.OPEN;
                        }
                    }
                }
                
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (level.getBlockState(pos.offset(dx, 0, dz)).is(ModBlocks.INVISIBLE_CAULDRON_BLOCK.get())) {
                            return BlockPathTypes.BLOCKED;
                        }
                    }
                }
                
                boolean isClimbable = state.is(BlockTags.CLIMBABLE);
                boolean isFloorClimbable = level.getBlockState(pos.below())
                .is(BlockTags.CLIMBABLE);

                if (isClimbable || isFloorClimbable) {
                    return BlockPathTypes.BLOCKED;
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
        this.isNearEnough = false;
        return super.moveTo(target.x, target.y, target.z, speed);
    }

    public boolean moveToCauldronPrecise(Vec3 target, float maxDist) {
        if (target == null) return false;
        double distSq = this.mob.position()
                .distanceToSqr(target.x, this.mob.getY(), target.z);

        double yDist = Math.abs(this.mob.getY() - target.y);

        if (distSq <= TOLERANCE_SQ && yDist < 1.5D) {
            this.stop();
            return true;
        }

        this.preciseMoveTo(target, 1.0D);
        return false;
    }

    @Override
    public boolean moveTo(Path path, double speed) {
        this.isNearEnough = false;
        this.preciseArrivalTicks = 0;
        this.stuckTicks = 0;
        return super.moveTo(path, speed);
    }

    @Override
    public void stop() {
        super.stop();
        this.preciseTarget = null;
        this.preciseArrivalTicks = 0;
        this.stuckTicks = 0;
        this.repathAttempts = 0;
    }

    @Override
    public void tick() {
        super.tick();
        
        if (DevMode.bogrePathfinding() &&
        this.level instanceof ServerLevel serverLevel &&
        this.tick % 5 == 0) {
            DefaultDebugRenderer.renderPath(serverLevel,
            this.path, this.preciseTarget, this.mob.position());
        }

        Vec3 currentPos = this.mob.position();
        boolean isPathActive = (!this.isDone() && this.path != null);
        boolean isPreciseArrival = (this.isDone() && this.preciseTarget != null);
        
        if (isPreciseArrival) {
            this.preciseArrivalTicks++;
            if (this.preciseArrivalTicks >= 30) {
                this.abortAndConsiderArrived();
                return;
            }
        } else {
            this.preciseArrivalTicks = 0;
        }
        
        if (isPathActive) {
            if (currentPos.distanceToSqr(this.lastPos) < STUCK_THRESHOLD_SQ &&
            !this.mob.isNoGravity()) {
                this.stuckTicks++;
                if (this.stuckTicks >= STUCK_REPATH_INTERVAL) {
                    if (this.repathAttempts < MAX_REPATH_ATTEMPTS) {
                        this.repathAttempts++;
                        this.stuckTicks = 0;
                        Path currentPath = this.path;
                        if (currentPath != null && currentPath.getTarget() != null) {
                            BlockPos target = currentPath.getTarget();
                            int savedAttempts = this.repathAttempts;
                            this.stop();
                            this.repathAttempts = savedAttempts;

                            this.moveTo(target.getX() + 0.5D,
                            target.getY(),
                            target.getZ() + 0.5D,
                            this.speedModifier);
                        }
                    } else {
                        this.abortAndConsiderArrived();
                    }
                }
            } else {
                this.stuckTicks = 0;
                this.repathAttempts = 0;
            }
        } else {
            this.stuckTicks = 0;
        }
        
        this.lastPos = currentPos;

        if (this.isDone() && this.preciseTarget != null) {
            handlePreciseArrival();
        }
    }

    private void abortAndConsiderArrived() {
        this.isNearEnough = true;
        this.stop();
        
        this.mob.getMoveControl().setWantedPosition(this.mob.getX(),
        this.mob.getY(),
        this.mob.getZ(), 0);
        this.mob.setZza(0.0F);
        this.mob.setXxa(0.0F);
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
    
    public boolean moveToCarvingTarget(Vec3 boneTarget, float minDistance, float maxDistance,
        List<BlockPos> carvableBlocks) {

        if (!(this.mob instanceof BogreEntity bogre)) return false;
        
        float targetDist = (minDistance + maxDistance) / 2.0f;
        BogreAi ai = bogre.getAi();
        Vec3 moveTarget;
        int lockedSide = ai.getSkillingMoveSide();
        
        if (ai.getSkillingMoveSide() != 99 && bogre.getAiTicks() > 100) {
            ai.setSkillingMoveSide(99);
        }

        if (ai.getSkillingMoveSide() == 0) {
            
            double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

            for (BlockPos bp : carvableBlocks) {
                if (bp.getX() < minX) minX = bp.getX();
                if (bp.getX() > maxX) maxX = bp.getX();
                if (bp.getZ() < minZ) minZ = bp.getZ();
                if (bp.getZ() > maxZ) maxZ = bp.getZ();
            }

            double spreadX = maxX - minX;
            double spreadZ = maxZ - minZ;

            List<SideChoice> frontSides = new ArrayList<>();
            List<SideChoice> endSides = new ArrayList<>();

            SideChoice posX = new SideChoice(1,
                new Vec3(boneTarget.x + targetDist, boneTarget.y, boneTarget.z),
                bogre.position().distanceToSqr(boneTarget.x + targetDist, bogre.getY(), boneTarget.z));

            SideChoice negX = new SideChoice(-1,
                new Vec3(boneTarget.x - targetDist, boneTarget.y, boneTarget.z),
                bogre.position().distanceToSqr(boneTarget.x - targetDist, bogre.getY(), boneTarget.z));

            SideChoice posZ = new SideChoice(2,
                new Vec3(boneTarget.x, boneTarget.y, boneTarget.z + targetDist),
                bogre.position().distanceToSqr(boneTarget.x, bogre.getY(), boneTarget.z + targetDist));
            
            SideChoice negZ = new SideChoice(-2,
                new Vec3(boneTarget.x, boneTarget.y, boneTarget.z - targetDist),
                bogre.position().distanceToSqr(boneTarget.x, bogre.getY(), boneTarget.z - targetDist));

            if (spreadX >= spreadZ) {
                // x -> front is z axis
                frontSides.add(posZ);
                frontSides.add(negZ);
                endSides.add(posX);
                endSides.add(negX);
            } else {
                //  z -> front is x axis
                frontSides.add(posX);
                frontSides.add(negX);
                endSides.add(posZ);
                endSides.add(negZ);
            }

            // sort each group by distance, prefer nearest
            frontSides.sort((c1, c2) -> Double.compare(c1.distSq, c2.distSq));
            endSides.sort((c1, c2) -> Double.compare(c1.distSq, c2.distSq));

            // try front sides first, then end sides as fallback
            List<SideChoice> ordered = new ArrayList<>();
            ordered.addAll(frontSides);
            ordered.addAll(endSides);

            for (SideChoice choice : ordered) {
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

        boolean isArrived = distToSpotSq <= 0.3 * 0.3 &&
        yDist <= 1.5D;

        if (this.isNearEnough && distToSpotSq <= 2.25D && yDist <= 1.5D) {
            isArrived = true;
        }

        if (!isArrived) {
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
