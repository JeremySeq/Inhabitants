package com.jeremyseq.inhabitants.entities.apex;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumSet;
import java.util.List;

public class ApexChargeAttackGoal extends Goal {
    private final ApexEntity mob;
    private final double chargeSpeed;
    private final int windupTicksStart;
    private final int maxChargeTicks;
    private final int stunTicksStart;

    private int windupTicks;
    private int chargeTicks;
    private int stunTicks;
    private Vec3 chargeDir;
    private boolean startedCharging = false;

    public ApexChargeAttackGoal(ApexEntity mob, double chargeSpeed, int windupTicksStart, int maxChargeTicks, int stunTicksStart) {
        this.mob = mob;
        this.chargeSpeed = chargeSpeed;
        this.windupTicksStart = windupTicksStart;
        this.maxChargeTicks = maxChargeTicks;
        this.stunTicksStart = stunTicksStart;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        // Don't start unless idle
        return mob.getState() == ApexEntity.State.IDLE;
    }

    @Override
    public void start() {
        this.windupTicks = this.windupTicksStart;
        this.chargeTicks = 0;
        this.stunTicks = 0;
        this.startedCharging = false;
        mob.setState(ApexEntity.State.WINDUP);
        mob.getNavigation().stop();
    }

    @Override
    public void stop() {
        mob.setState(ApexEntity.State.IDLE);
        this.startedCharging = false;
        this.chargeTicks = 0;
        this.windupTicks = 0;
        this.stunTicks = 0;
        // clear motion
        this.mob.setDeltaMovement(Vec3.ZERO);
        mob.setSprinting(false);
        mob.getNavigation().stop();
    }

    @Override
    public boolean canContinueToUse() {
        // Continue while charging or winding up or stunned cooldown
        return (this.windupTicks >= 0) || this.startedCharging || (this.stunTicks >= 0);
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            stop();
            return;
        }

        // Windup phase
        if (windupTicks >= 0) {
            windupTicks--;
            // keep facing target
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

            mob.playSound(SoundEvents.COW_STEP, 0.5F, 1.0F);
            if (windupTicks == 0) {
                // start charging
                startedCharging = true;
                // compute direction once at the start
                Vec3 toTarget = target.position().subtract(mob.position()).normalize();
                this.chargeDir = new Vec3(toTarget.x, 0.0, toTarget.z).normalize();
                this.setApexRot(chargeDir);

                // ensure sprint for speed scaling if needed
                mob.setSprinting(true);
                this.chargeTicks = 0;
                mob.setState(ApexEntity.State.CHARGING);
            }
            return;
        }

        // Stunned phase
        if (stunTicks >= 0) {
            stunTicks--;
            if (stunTicks == 0) {
                mob.setState(ApexEntity.State.IDLE);
            }
            return;
        }
        // Charging phase
        if (startedCharging) {
            // move forward with a fixed velocity each tick
            double vy = mob.getDeltaMovement().y;
            Vec3 motion = new Vec3(this.chargeDir.x * chargeSpeed, vy, this.chargeDir.z * chargeSpeed);
            mob.setDeltaMovement(motion);

            // update rotation to face charge direction
            this.setApexRot(chargeDir);

            // check blocks in front using an AABB placed ahead of the apex (only check every few ticks for performance)
            if (mob.tickCount % 10 == 0) {
                AABB frontBox = mob.getBoundingBox().move(this.chargeDir.scale(chargeSpeed)).inflate(0.5, 0.0, 0.5);
                if (isAnySolidBlockInAABB(frontBox)) {
                    this.stun();
                    return;
                }
            }

            // hit an entity -> deal damage and stop charge
            List<LivingEntity> list = mob.level().getEntitiesOfClass(LivingEntity.class,
                    mob.getBoundingBox().inflate(1.5D).move(motion),
                    (e) -> e != mob && e.isAlive());
            if (!list.isEmpty()) {
                LivingEntity hitEntity = list.get(0);

                // compute angle between mob facing and vector to target, require within ~30deg cone
                Vec3 toHit = hitEntity.position().subtract(mob.position()).normalize();
                Vec3 facing = Vec3.directionFromRotation(0.0F, mob.getYRot()).normalize();
                double dot = facing.dot(toHit);
                // 30 degree threshold
                double threshold = Math.cos(Math.toRadians(30));

                if (dot > threshold) {
                    mob.doHurtTarget(hitEntity);

                    // if holding a shield, damage it upon impact and become stunned
                    if (hitEntity.isBlocking()) {
                        for (InteractionHand hand : InteractionHand.values()) {
                            ItemStack stack = hitEntity.getItemInHand(hand);
                            if (stack.getItem() instanceof ShieldItem) {
                                stack.hurtAndBreak(stack.getMaxDamage(), hitEntity, p -> p.broadcastBreakEvent(hand));
                                break;
                            }
                        }
                        this.stun();
                        return;
                    }
                }
            }

            // increment charge counter and stop if exceeded
            chargeTicks++;
            if (chargeTicks >= maxChargeTicks) {
                mob.setState(ApexEntity.State.IDLE);
                mob.setSprinting(false);
                this.startedCharging = false;
                mob.getNavigation().stop();
            }
        }
    }

    public void setApexRot(Vec3 dir) {
        // set mob rotations to face charge direction
        float yaw = (float) Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0F;
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setXRot(0.0F);
    }

    public void stun() {
        mob.setState(ApexEntity.State.STUNNED);
        this.stunTicks = this.stunTicksStart;
        this.startedCharging = false;
        mob.setDeltaMovement(Vec3.ZERO);
        mob.setSprinting(false);
    }

    // Scan integer block positions inside the AABB and return true if any block has a non-empty collision shape
    private boolean isAnySolidBlockInAABB(AABB box) {
        int minX = (int)Math.floor(box.minX);
        int minY = (int)Math.floor(box.minY);
        int minZ = (int)Math.floor(box.minZ);
        int maxX = (int)Math.floor(box.maxX);
        int maxY = (int)Math.floor(box.maxY);
        int maxZ = (int)Math.floor(box.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = mob.level().getBlockState(pos);
                    VoxelShape shape = state.getCollisionShape(mob.level(), pos);
                    if (!shape.isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
