package com.jeremyseq.inhabitants.entities.bulltoad;

import com.google.common.collect.Lists;
import com.jeremyseq.inhabitants.entities.goals.PathfindingOvershootFix;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;

public class BulltoadMoveControl extends MoveControl {

    private final BulltoadEntity bulltoad;
    private int jumpCooldown = 0;
    private static final int JUMP_COOLDOWN = 50;
    private static final float MAX_JUMP_DIST = 6f;
    private static final float MAX_JUMP_VELOCITY = 3f;
    private static final List<Integer> ALLOWED_ANGLES = Lists.newArrayList(65, 70, 75, 80);

    private double jumpDx = 0;
    private double jumpDz = 0;
    private float jumpYaw = 0;

    public BulltoadMoveControl(BulltoadEntity bulltoad) {
        super(bulltoad);
        this.bulltoad = bulltoad;
    }

    @Override
    public void tick() {
        // landing
        if (bulltoad.onGround() && jumpCooldown <= JUMP_COOLDOWN - 10) {
            bulltoad.getEntityData().set(BulltoadEntity.JUMPING, false);
            bulltoad.setDiscardFriction(false);
            jumpDx = 0;
            jumpDz = 0;
        }
        if (jumpCooldown > 0) jumpCooldown--;

        // jump momentum midair
        if (!bulltoad.onGround() && (jumpDx != 0 || jumpDz != 0)) {
            jumpDx *= 0.98;
            jumpDz *= 0.98;
            bulltoad.setDeltaMovement(jumpDx, bulltoad.getDeltaMovement().y, jumpDz);
            bulltoad.setYRot(jumpYaw);
            bulltoad.yRotO = jumpYaw;
            bulltoad.yHeadRot = jumpYaw;
            bulltoad.yHeadRotO = jumpYaw;
            bulltoad.setYBodyRot(jumpYaw);
        }

        if (!bulltoad.onGround()) {
            PathfindingOvershootFix.overshootFix(bulltoad);
            return;
        }
        // if jump cooldown isn't finished, just walk
        if (jumpCooldown > 0) {
            super.tick();
            return;
        }

        // walk if close enough to target
        if (bulltoad.getNavigation().getTargetPos() != null &&
                bulltoad.getNavigation().getTargetPos().distToCenterSqr(bulltoad.position()) < 25) {
            super.tick();
            return;
        }

        Vec3 targetPos = findTargetPos();
        if (targetPos == null) {
            super.tick();
            return;
        }

        performJump(targetPos);
        jumpCooldown = JUMP_COOLDOWN;
    }

    private Vec3 findTargetPos() {
        Vec3 rawTarget;
        if (!bulltoad.getNavigation().isDone() && bulltoad.getNavigation().getTargetPos() != null) {
            rawTarget = bulltoad.getNavigation().getTargetPos().getCenter();
            Vec3 toTarget = rawTarget.subtract(bulltoad.position());
            if (toTarget.length() > MAX_JUMP_DIST) {
                rawTarget = bulltoad.position().add(toTarget.normalize().scale(MAX_JUMP_DIST));
            }
        } else {
            return null;
        }

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
        for (int dy = 5; dy >= -4; dy--) {
            BlockPos check = pos.offset(0, dy, 0);
            if (bulltoad.level().getBlockState(check.below()).isSolidRender(bulltoad.level(), check.below())
                    && bulltoad.level().getBlockState(check).isAir()) {
                return check;
            }
        }
        return null;
    }

    private boolean isAcceptableLandingSpot(BlockPos pos) {
        Level level = bulltoad.level();
        BlockPos below = pos.below();
        if (!level.getFluidState(pos).isEmpty()) return false;
        if (!level.getFluidState(below).isEmpty()) return false;
        if (!level.getFluidState(pos.above()).isEmpty()) return false;
        if (!level.getBlockState(below).isSolidRender(level, below)) return false;
        BlockPathTypes type = WalkNodeEvaluator.getBlockPathTypeStatic(level, pos.mutable());
        return bulltoad.getPathfindingMalus(type) == 0.0F;
    }

    private void performJump(Vec3 targetPos) {
        Vec3 jumpVec = calculateOptimalJumpVector(targetPos);
        if (jumpVec == null) return;

        jumpDx = jumpVec.x;
        jumpDz = jumpVec.z;
        jumpYaw = (float) Math.toDegrees(Math.atan2(-jumpDx, jumpDz));

        bulltoad.setYRot(jumpYaw);
        bulltoad.setDiscardFriction(true);
        bulltoad.setDeltaMovement(jumpVec);
        bulltoad.getEntityData().set(BulltoadEntity.JUMPING, true);
    }

    private Vec3 calculateOptimalJumpVector(Vec3 pTarget) {
        List<Integer> list = Lists.newArrayList(ALLOWED_ANGLES);
        Collections.shuffle(list);
        for (int i : list) {
            Vec3 vec = calculateJumpVectorForAngle(pTarget, i);
            if (vec != null) return vec;
        }
        return null;
    }

    private Vec3 calculateJumpVectorForAngle(Vec3 pTarget, int pAngle) {
        Vec3 vec3 = bulltoad.position();
        Vec3 vec31 = new Vec3(pTarget.x - vec3.x, 0, pTarget.z - vec3.z).normalize().scale(0.5);
        pTarget = pTarget.subtract(vec31);
        Vec3 vec32 = pTarget.subtract(vec3);
        float f = pAngle * (float) Math.PI / 180f;
        double d0 = Math.atan2(vec32.z, vec32.x);
        double d1 = vec32.subtract(0, vec32.y, 0).lengthSqr();
        double d2 = Math.sqrt(d1);
        double d3 = vec32.y;
        double d4 = Math.sin(2f * f);
        double d6 = Math.pow(Math.cos(f), 2);
        double d7 = Math.sin(f);
        double d8 = Math.cos(f);
        double d9 = Math.sin(d0);
        double d10 = Math.cos(d0);
        double d11 = d1 * 0.08 / (d2 * d4 - 2 * d3 * d6);
        if (d11 < 0) return null;
        double d12 = Math.sqrt(d11);
        if (d12 > MAX_JUMP_VELOCITY) return null;
        double d13 = d12 * d8;
        double d14 = d12 * d7;
        int i = Mth.ceil(d2 / d13) * 2;
        double d15 = 0;
        Vec3 vec33 = null;
        EntityDimensions dims = bulltoad.getDimensions(Pose.LONG_JUMPING);
        for (int j = 0; j < i - 1; j++) {
            d15 += d2 / i;
            double d16 = d7 / d8 * d15 - Math.pow(d15, 2) * 0.08 / (2 * d11 * Math.pow(d8, 2));
            Vec3 vec34 = new Vec3(vec3.x + d15 * d10, vec3.y + d16, vec3.z + d15 * d9);
            if (vec33 != null && !isClearTransition(dims, vec33, vec34)) return null;
            vec33 = vec34;
        }
        return new Vec3(d13 * d10, d14, d13 * d9).scale(0.95f);
    }

    private boolean isClearTransition(EntityDimensions dims, Vec3 start, Vec3 end) {
        Vec3 vec3 = end.subtract(start);
        double d0 = Math.min(dims.width, dims.height);
        int i = Mth.ceil(vec3.length() / d0);
        Vec3 vec31 = vec3.normalize();
        Vec3 vec32 = start;
        for (int j = 0; j < i; j++) {
            vec32 = j == i - 1 ? end : vec32.add(vec31.scale(d0 * 0.9));
            if (!bulltoad.level().noCollision(bulltoad, dims.makeBoundingBox(vec32))) return false;
        }
        return true;
    }
}