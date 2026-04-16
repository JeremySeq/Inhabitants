package com.jeremyseq.inhabitants.entities.impaler;

import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import com.jeremyseq.inhabitants.effects.ModEffects;
import com.jeremyseq.inhabitants.entities.EntityUtil;
import com.jeremyseq.inhabitants.networking.ModNetworking;
import com.jeremyseq.inhabitants.networking.TinnitusPacketS2C;
import com.jeremyseq.inhabitants.damagesource.ModDamageTypes;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.phys.Vec3;
import static net.minecraft.world.level.block.PointedDripstoneBlock.THICKNESS;
import static net.minecraft.world.level.block.PointedDripstoneBlock.TIP_DIRECTION;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public class ImpalerScreamGoal extends Goal {
    private final ImpalerEntity mob;
    private int screamTimer = 0;
    private final Set<UUID> hitEntities = new HashSet<>();

    private static final double boxMaxLength = 14.0d;
    private static final double boxWidth = 5.0d;
    private static final double boxHeight = 4.0d;

    private static final double dripRadius = 15.0d;
    private static final double dripHeight = 20.0d;
    private static final int maxDripDrops = 20;

    public ImpalerScreamGoal(ImpalerEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return mob.getTarget() != null && mob.screamCooldown == 0
                && mob.getTarget().distanceToSqr(mob) <= 36 && mob.getTarget().distanceToSqr(mob) >= 16;
    }

    @Override
    public boolean canContinueToUse() {
        return screamTimer <= 20;
    }

    @Override
    public void start() {
        screamTimer = 0;
        hitEntities.clear();
        if (mob.getTarget() != null) {
            mob.lookAt(EntityAnchorArgument.Anchor.FEET, mob.getTarget().getPosition(0));
        }
        mob.triggerAnim("scream", "scream");
        mob.playSound(ModSoundEvents.IMPALER_SCREAM.get(), 1.0F, 1.0F);
    }

    @Override
    public void stop() {
        mob.screamCooldown = ImpalerEntity.SCREAM_COOLDOWN;
        mob.getEntityData().set(ImpalerEntity.SCREAM_START_TICK, -1);
    }

    @Override
    public void tick() {
        screamTimer++;

        // make sure not moving during scream
        mob.getNavigation().stop();
        mob.setDeltaMovement(Vec3.ZERO);

        if (screamTimer >= 6 && screamTimer <= 16) {
            if (screamTimer == 6) {
                // trigger client stuff
                mob.getEntityData().set(ImpalerEntity.SCREAM_START_TICK, (int) mob.level().getGameTime());
                
                triggerDripstoneFall();
            }

            // expanding box detection [O+++++]
            double progress = (screamTimer - 6) / 10.0;
            double currentLength = boxMaxLength * progress;

            List<LivingEntity> targets = mob.level().getEntitiesOfClass(
                LivingEntity.class, mob.getBoundingBox().inflate(boxMaxLength));
            
            for (LivingEntity target : targets) {
                if (target != mob &&
                !(target instanceof ImpalerEntity) &&
                !target.isSpectator() &&
                target.isAlive() &&
                !hitEntities.contains(target.getUUID())) {
                    if (EntityUtil.isInScreamBox(mob, target, currentLength, boxWidth, boxHeight)) {
                        applyScreamEffect(target);
                        hitEntities.add(target.getUUID());
                    }
                }
            }
        }
    }

    private void applyScreamEffect(LivingEntity target) {
        // Concussion effect
        if (target instanceof Player player) {
            if (!player.isCreative() && !player.isSpectator()) {
                player.addEffect(new MobEffectInstance(ModEffects.CONCUSSION.get(), 240, 0));
                ModNetworking.sendToPlayer(new TinnitusPacketS2C(), (ServerPlayer) player);
            }
        }

        // Knockback
        Vec3 knockbackDir = target.position().subtract(mob.position()).normalize();
        target.knockback(1.5, -knockbackDir.x, -knockbackDir.z);
        target.hurt(ModDamageTypes.causeImpaledDamage(mob.level(), mob), 4.0f);
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
