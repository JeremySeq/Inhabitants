package com.jeremyseq.inhabitants.entities.bulltoad.goals;

import com.google.common.collect.Lists;
import com.jeremyseq.inhabitants.debug.DevMode;
import com.jeremyseq.inhabitants.entities.bulltoad.BulltoadEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class BulltoadJumpGoal extends Goal {

    private final BulltoadEntity bulltoad;
    private int jumpCooldown = 0;

    private double jumpDx = 0;
    private double jumpDz = 0;
    private float jumpYaw = 0;

    private static final int JUMP_COOLDOWN = 50;
    private final float MAX_JUMP_DIST = 6f;
    private final float MAX_JUMP_VELOCITY = 3f;

    Vec3 targetPos = null;

    private static final List<Integer> ALLOWED_ANGLES = Lists.newArrayList(65, 70, 75, 80);

    public BulltoadJumpGoal(BulltoadEntity bulltoad) {
        this.bulltoad = bulltoad;
        this.setFlags(EnumSet.of(Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!bulltoad.onGround()) return false;
        if (jumpCooldown > 0) {
            jumpCooldown--;
            return false;
        }

        boolean wantsMove = !bulltoad.getNavigation().isDone();
        if (!wantsMove && bulltoad.getRandom().nextFloat() >= 0.12f) return false;

        // Always compute targetPos before deciding
        targetPos = findTargetPos();
        return targetPos != null;
    }

    @Override
    public void start() {

        performJump(targetPos);
        jumpCooldown = JUMP_COOLDOWN;
    }

    private Vec3 findTargetPos() {
        Vec3 rawTarget;
        if (!bulltoad.getNavigation().isDone()) {
            rawTarget = Objects.requireNonNull(bulltoad.getNavigation().getTargetPos()).getCenter();
            Vec3 toTarget = rawTarget.subtract(bulltoad.position());
            if (toTarget.length() > MAX_JUMP_DIST) {
                rawTarget = bulltoad.position().add(toTarget.normalize().scale(MAX_JUMP_DIST));
            }
        } else {
            float randYaw = bulltoad.getYRot() + (bulltoad.getRandom().nextFloat() - 0.5f) * 90f;
            double rrad = Math.toRadians(randYaw);
            double dx = -Math.sin(rrad) * MAX_JUMP_DIST;
            double dz = Math.cos(rrad) * MAX_JUMP_DIST;
            rawTarget = bulltoad.position().add(dx, 0, dz);
        }

        // Search 3x3 around target, snapping each candidate to the ground
        BlockPos blockTarget = BlockPos.containing(rawTarget.x, rawTarget.y, rawTarget.z);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = snapToGround(blockTarget.offset(x, 0, z));
                if (checkPos != null && isAcceptableLandingSpot(checkPos)) {
                    return Vec3.atBottomCenterOf(checkPos);
                }
            }
        }

        return null;
    }

    private BlockPos snapToGround(BlockPos pos) {
        // Search up to 4 blocks down and 2 blocks up for a valid surface
        for (int dy = 2; dy >= -4; dy--) {
            BlockPos check = pos.offset(0, dy, 0);
            if (bulltoad.level().getBlockState(check.below()).isSolidRender(bulltoad.level(), check.below())
                    && bulltoad.level().getBlockState(check).isAir()) {
                return check;
            }
        }
        return null;
    }

    @Override
    public boolean canContinueToUse() {
        return bulltoad.getEntityData().get(BulltoadEntity.JUMPING);
    }

    @Override
    public void tick() {
        if (bulltoad.onGround() && jumpCooldown <= JUMP_COOLDOWN - 10) {
            bulltoad.getEntityData().set(BulltoadEntity.JUMPING, false);
            bulltoad.setDiscardFriction(false);
            jumpDx = 0;
            jumpDz = 0;
        }
        if (jumpCooldown > 0) jumpCooldown--;

        if (!bulltoad.onGround() && (jumpDx != 0 || jumpDz != 0)) {
            jumpDx *= 0.98;
            jumpDz *= 0.98;
            bulltoad.setDeltaMovement(jumpDx, bulltoad.getDeltaMovement().y, jumpDz);
            bulltoad.setYRot(jumpYaw);
            bulltoad.yRotO = jumpYaw;
            bulltoad.yHeadRot = jumpYaw;
        }
    }

    private void performJump(Vec3 targetPos) {
        Vec3 optimalJumpVector = calculateOptimalJumpVector(bulltoad, targetPos);

        if (optimalJumpVector == null) {
            return;
        }

        jumpDx = optimalJumpVector.x;
        jumpDz = optimalJumpVector.z;
        jumpYaw = (float) Math.toDegrees(Math.atan2(-jumpDx, jumpDz));

        bulltoad.setYRot(jumpYaw);
        bulltoad.setDiscardFriction(true);
        bulltoad.setDeltaMovement(optimalJumpVector);
        bulltoad.getEntityData().set(BulltoadEntity.JUMPING, true);

        if (DevMode.bulltoadJumpTarget()) spawnTargetParticle(targetPos);
    }

    private void spawnTargetParticle(Vec3 pos) {
        if (bulltoad.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z,
                    8, 0.3, 0.3,0.3,0.0);
        }
    }

    /**
     * The methods below are copied from the jumping used in vanilla frog and goat.
     * @see net.minecraft.world.entity.ai.behavior.LongJumpToRandomPos
     */


    @Nullable
    protected Vec3 calculateOptimalJumpVector(Mob pMob, Vec3 pTarget) {
        List<Integer> list = Lists.newArrayList(ALLOWED_ANGLES);
        Collections.shuffle(list);

        for(int i : list) {
            Vec3 vec3 = this.calculateJumpVectorForAngle(pMob, pTarget, i);
            if (vec3 != null) {
                return vec3;
            }
        }

        return null;
    }

    private boolean isAcceptableLandingSpot(BlockPos pos) {
        Level level = bulltoad.level();
        BlockPos below = pos.below();

        // No fluids at landing spot or above
        if (!level.getFluidState(pos).isEmpty()) return false;
        if (!level.getFluidState(below).isEmpty()) return false;
        if (!level.getFluidState(pos.above()).isEmpty()) return false;

        // Solid full-cube surface to land on
        if (!level.getBlockState(below).isSolidRender(level, below)) return false;

        // No pathfinding penalty at landing position (catches lava, fire, etc.)
        BlockPathTypes type = WalkNodeEvaluator.getBlockPathTypeStatic(level, pos.mutable());
        return bulltoad.getPathfindingMalus(type) == 0.0F;
    }

    private Vec3 calculateJumpVectorForAngle(Mob pMob, Vec3 pTarget, int pAngle) {
        Vec3 vec3 = pMob.position();
        Vec3 vec31 = (new Vec3(pTarget.x - vec3.x, 0.0D, pTarget.z - vec3.z)).normalize().scale(0.5D);
        pTarget = pTarget.subtract(vec31);
        Vec3 vec32 = pTarget.subtract(vec3);
        float f = (float)pAngle * (float)Math.PI / 180.0F;
        double d0 = Math.atan2(vec32.z, vec32.x);
        double d1 = vec32.subtract(0.0D, vec32.y, 0.0D).lengthSqr();
        double d2 = Math.sqrt(d1);
        double d3 = vec32.y;
        double d4 = Math.sin(2.0F * f);
        double d6 = Math.pow(Math.cos(f), 2.0D);
        double d7 = Math.sin(f);
        double d8 = Math.cos(f);
        double d9 = Math.sin(d0);
        double d10 = Math.cos(d0);
        double d11 = d1 * 0.08D / (d2 * d4 - 2.0D * d3 * d6);
        if (d11 < 0.0D) {
            return null;
        } else {
            double d12 = Math.sqrt(d11);
            if (d12 > (double)this.MAX_JUMP_VELOCITY) {
                return null;
            } else {
                double d13 = d12 * d8;
                double d14 = d12 * d7;
                int i = Mth.ceil(d2 / d13) * 2;
                double d15 = 0.0D;
                Vec3 vec33 = null;
                EntityDimensions entitydimensions = pMob.getDimensions(Pose.LONG_JUMPING);

                for(int j = 0; j < i - 1; ++j) {
                    d15 += d2 / (double)i;
                    double d16 = d7 / d8 * d15 - Math.pow(d15, 2.0D) * 0.08D / (2.0D * d11 * Math.pow(d8, 2.0D));
                    double d17 = d15 * d10;
                    double d18 = d15 * d9;
                    Vec3 vec34 = new Vec3(vec3.x + d17, vec3.y + d16, vec3.z + d18);
                    if (vec33 != null && !this.isClearTransition(pMob, entitydimensions, vec33, vec34)) {
                        return null;
                    }

                    vec33 = vec34;
                }

                return (new Vec3(d13 * d10, d14, d13 * d9)).scale(0.95F);
            }
        }
    }

    private boolean isClearTransition(Mob pMob, EntityDimensions pDimensions, Vec3 pStart, Vec3 pEnd) {
        Vec3 vec3 = pEnd.subtract(pStart);
        double d0 = Math.min(pDimensions.width, pDimensions.height);
        int i = Mth.ceil(vec3.length() / d0);
        Vec3 vec31 = vec3.normalize();
        Vec3 vec32 = pStart;

        for(int j = 0; j < i; ++j) {
            vec32 = j == i - 1 ? pEnd : vec32.add(vec31.scale(d0 * (double)0.9F));
            if (!pMob.level().noCollision(pMob, pDimensions.makeBoundingBox(vec32))) {
                return false;
            }
        }

        return true;
    }
}