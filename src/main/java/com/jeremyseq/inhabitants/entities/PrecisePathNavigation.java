package com.jeremyseq.inhabitants.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PrecisePathNavigation extends GroundPathNavigation {

    private Vec3 preciseTarget = null;
    private static final double TOLERANCE_SQ = 0.20D * 0.20D;

    public PrecisePathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    /**
     * A precise version of {@link #moveTo(double, double, double, double)}.
     * Ensures the entity will end up exactly at the target position.
     */
    public boolean preciseMoveTo(Vec3 target, double speed) {
        this.preciseTarget = target;
        return super.moveTo(target.x, target.y, target.z, speed);
    }

    @Override
    public void tick() {
        ++this.tick;
        if (this.hasDelayedRecomputation) this.recomputePath();

        if (!this.isDone()) {
            if (this.canUpdatePath()) {
                this.followThePath();
            } else if (this.path != null && !this.path.isDone()) {
                Vec3 mobPos = this.getTempMobPos();
                Vec3 next    = this.path.getNextEntityPos(this.mob);
                if (mobPos.y > next.y && !this.mob.onGround()
                        && Mth.floor(mobPos.x) == Mth.floor(next.x)
                        && Mth.floor(mobPos.z) == Mth.floor(next.z)) {
                    this.path.advance();
                }
            }

            DebugPackets.sendPathFindingPacket(this.level, this.mob, this.path,
                    this.maxDistanceToWaypoint);

            if (!this.isDone()) {
                assert this.path != null;
                Vec3 next = this.path.getNextEntityPos(this.mob);
                this.mob.getMoveControl()
                        .setWantedPosition(next.x, getGroundY(next), next.z,
                                this.speedModifier);
                return;
            }
        }

        if (this.preciseTarget != null) {
            double distSq = this.mob.position().distanceToSqr(this.preciseTarget);

            if (distSq > TOLERANCE_SQ) {
                this.mob.getMoveControl()
                        .setWantedPosition(preciseTarget.x,
                                getGroundY(preciseTarget),
                                preciseTarget.z,
                                this.speedModifier);
                return;
            }

            this.mob.setPos(preciseTarget.x, preciseTarget.y, preciseTarget.z);
            this.stop();
            this.preciseTarget = null;
        }
    }

    @Override
    protected void followThePath() {
        Vec3 mobPos = this.getTempMobPos();
        this.maxDistanceToWaypoint =
                Math.max(0.05F, this.mob.getBbWidth() * 0.5F + 0.01F);

        assert this.path != null;
        BlockPos nodePos   = this.path.getNextNodePos();
        double    centerX  = nodePos.getX() + 0.5D;
        double    centerZ  = nodePos.getZ() + 0.5D;

        boolean arrived =
                Math.abs(this.mob.getX() - centerX) <= this.maxDistanceToWaypoint &&
                        Math.abs(this.mob.getZ() - centerZ) <= this.maxDistanceToWaypoint &&
                        Math.abs(this.mob.getY() - nodePos.getY()) < 1.0D;

        if (arrived || (this.canCutCorner(this.path.getNextNode().type)
                && shouldTargetNextNodeInDirection(mobPos))) {
            this.path.advance();
        }

        this.doStuckDetection(mobPos);
    }

    private boolean shouldTargetNextNodeInDirection(Vec3 pos) {
        assert this.path != null;
        if (this.path.getNextNodeIndex() + 1 >= this.path.getNodeCount()) return false;

        Vec3 nextNode = Vec3.atBottomCenterOf(this.path.getNextNodePos());
        if (!pos.closerThan(nextNode, 2.0D)) return false;

        if (this.canMoveDirectly(pos, this.path.getNextEntityPos(this.mob))) return true;

        Vec3 afterNext = Vec3.atBottomCenterOf(
                this.path.getNodePos(this.path.getNextNodeIndex() + 1));

        Vec3 toNext     = nextNode.subtract(pos);
        Vec3 toAfter    = afterNext.subtract(pos);
        double lenNext  = toNext.lengthSqr();
        double lenAfter = toAfter.lengthSqr();

        if (lenAfter >= lenNext && lenNext >= 0.5D) return false;
        return toAfter.normalize().dot(toNext.normalize()) < 0.0D;
    }
}
