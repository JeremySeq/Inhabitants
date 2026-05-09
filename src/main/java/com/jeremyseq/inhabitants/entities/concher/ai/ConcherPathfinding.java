package com.jeremyseq.inhabitants.entities.concher.ai;

import com.jeremyseq.inhabitants.entities.concher.ConcherEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.pathfinder.AmphibiousNodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;

public class ConcherPathfinding {

    public static Vec3 findValidWanderTarget(ConcherEntity concher) {
        Vec3 pos;

        if (concher.isInWater()) {
            pos = BehaviorUtils.getRandomSwimmablePos(concher, 10, 7);
        } else {
            pos = DefaultRandomPos.getPos(concher, 5, 4);
        }

        if (pos != null) {
            BlockPos bPos = BlockPos.containing(pos);
            Level level = concher.level();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos checkPos = bPos.offset(dx, 0, dz);

                    if (!level.getBlockState(checkPos).getCollisionShape(level, checkPos).isEmpty()) {
                        return null;
                    }
                }
            }
        }
        return pos;
    }

    public static class ConcherPathNavigation extends AmphibiousPathNavigation {
        public ConcherPathNavigation(ConcherEntity concher, Level level) {
            super(concher, level);
        }

        @Override
        protected PathFinder createPathFinder(int pRange) {
            this.nodeEvaluator = new AmphibiousNodeEvaluator(false);
            return new PathFinder(this.nodeEvaluator, pRange);
        }
    }

    public static class ConcherMoveControl extends MoveControl {
        private final ConcherEntity concher;
        private int internalTicks = 0;

        public ConcherMoveControl(ConcherEntity concher) {
            super(concher);
            this.concher = concher;
        }

        @Override
        public void tick() {
            if (this.concher.getAIState() == ConcherAi.State.PANIC && this.concher.getStage() > 0) {
                this.concher.setSpeed(0.0f);
                this.concher.setXxa(0.0f);
                this.concher.setYya(0.0f);
                this.concher.setZza(0.0f);
                return;
            }

            if (this.concher.isInWater() && this.concher.getStage() == 0) {
                handleWaterMovement();
            } else {
                handleLandMovement();
            }
        }

        private void handleWaterMovement() {
            if (this.operation == Operation.MOVE_TO &&
                    !this.concher.getNavigation().isDone()) {

                double dx = this.wantedX - this.concher.getX();
                double dy = this.wantedY - this.concher.getY();
                double dz = this.wantedZ - this.concher.getZ();
                double distSq = dx * dx + dy * dy + dz * dz;

                if (distSq < 0.0000001d) {
                    this.concher.setZza(0.0f);
                    return;
                }

                float rotSpeed = this.concher.getAI().getRotationSpeed() * 100.0f;
                float f = (float) (Mth.atan2(dz, dx) * (180d / Math.PI)) - 90.0f;

                this.concher.setYRot(this.rotlerp(this.concher.getYRot(), f, rotSpeed));
                this.concher.yBodyRot = this.concher.getYRot();
                this.concher.yHeadRot = this.concher.getYRot();

                float speedAttr = (float) (this.speedModifier *
                        this.concher.getAttributeValue(Attributes.MOVEMENT_SPEED));

                this.concher.setSpeed(speedAttr * 0.1f);

                double d4 = Math.sqrt(dx * dx + dz * dz);
                if (Math.abs(dy) > 0.00001d || Math.abs(d4) > 0.00001d) {
                    float f1 = (float) (-(Mth.atan2(dy, d4) * (180d / Math.PI)));
                    this.concher.setXRot(this.rotlerp(this.concher.getXRot(), f1, rotSpeed));
                }

                float fCos = Mth.cos(this.concher.getXRot() * ((float) Math.PI / 180f));
                float fSin = Mth.sin(this.concher.getXRot() * ((float) Math.PI / 180f));

                this.concher.zza = fCos * speedAttr;
                this.concher.yya = -fSin * speedAttr;

                if (this.concher.horizontalCollision && dy > 0) {
                    this.concher.getJumpControl().jump();
                }
            } else {
                this.concher.setSpeed(0.0f);
                this.concher.setXxa(0.0f);
                this.concher.setYya(0.0f);
                this.concher.setZza(0.0f);
            }
        }

        private void handleLandMovement() {
            if (this.operation == Operation.MOVE_TO &&
                    !this.concher.getNavigation().isDone()) {

                internalTicks++;

                int pauseThreshold = this.concher.getAI().getPreWalkTicks();
                int walkTicks = this.concher.getAI().getWalkTicks();
                int cycle = pauseThreshold + walkTicks;

                int timing = internalTicks % cycle;
                boolean isPausing = timing < pauseThreshold;

                double dx = this.wantedX - this.concher.getX();
                double dy = this.wantedY - this.concher.getY();
                double dz = this.wantedZ - this.concher.getZ();

                float rotSpeed = this.concher.getAI().getRotationSpeed() * 100.0f;
                float targetYaw = (float) (Mth.atan2(dz, dx) * (180d / Math.PI)) - 90.0f;

                this.concher.setYRot(this.rotlerp(this.concher.getYRot(), targetYaw, rotSpeed));
                this.concher.yBodyRot = this.concher.getYRot();
                this.concher.yHeadRot = this.concher.getYRot();

                if (isPausing) {
                    this.concher.setSpeed(0.0f);
                    this.concher.setZza(0.0f);
                    this.concher.setXxa(0.0f);
                    this.concher.setDeltaMovement(0, this.concher.getDeltaMovement().y, 0);
                } else {
                    float speed = (float) (this.speedModifier *
                            this.concher.getAttributeValue(Attributes.MOVEMENT_SPEED));

                    this.concher.setSpeed(speed);
                }

                if (this.concher.isWalkPausing() != isPausing) {
                    this.concher.setWalkPausing(isPausing);
                }
            } else {
                internalTicks = 0;
                this.concher.setSpeed(0.0f);

                if (!this.concher.isWalkPausing()) {
                    this.concher.setWalkPausing(true);
                }
            }
        }

        public boolean isPausing() {
            return this.concher.isWalkPausing();
        }
    }
}
