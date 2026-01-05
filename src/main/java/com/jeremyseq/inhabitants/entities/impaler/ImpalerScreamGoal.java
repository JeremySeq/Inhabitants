package com.jeremyseq.inhabitants.entities.impaler;

import com.jeremyseq.inhabitants.effects.ModEffects;
import com.jeremyseq.inhabitants.entities.EntityUtil;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

import static net.minecraft.world.level.block.PointedDripstoneBlock.THICKNESS;
import static net.minecraft.world.level.block.PointedDripstoneBlock.TIP_DIRECTION;

public class ImpalerScreamGoal extends Goal {
    private final ImpalerEntity mob;
    private int screamTimer = 0;

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
        if (mob.getTarget() != null) {
            mob.lookAt(EntityAnchorArgument.Anchor.FEET, mob.getTarget().getPosition(0));
        }
        mob.triggerAnim("scream", "scream");
    }

    @Override
    public void stop() {
        mob.screamCooldown = ImpalerEntity.SCREAM_COOLDOWN;
    }

    @Override
    public void tick() {
        screamTimer++;

        // make sure not moving during scream
        mob.getNavigation().stop();
        mob.setDeltaMovement(Vec3.ZERO);

        if (screamTimer == 6) {
            // trigger client stuff
            mob.getEntityData().set(ImpalerEntity.SCREAM_TRIGGER, false);
            mob.getEntityData().set(ImpalerEntity.SCREAM_TRIGGER, true);

            EntityUtil.shockwave(mob, 10, 10, entity -> entity instanceof ImpalerEntity);
            mob.level().playSound(null, mob.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 10f, 0.9F);
            // give nearby players the concussion effect
            double radius = 15.0D;
            List<Player> players = mob.level().getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(radius));
            for (Player player : players) {
                player.addEffect(new MobEffectInstance(ModEffects.CONCUSSION.get(), 200, 0));
            }

            // make pointed dripstone attached to ceilings fall
            double dripRadius = 15.0D;
            int maxToDrop = 20;
            int dropped = 0;
            int r = (int) Math.ceil(dripRadius);
            for (int dx = -r; dx <= r && dropped < maxToDrop; dx++) {
                for (int dz = -r; dz <= r && dropped < maxToDrop; dz++) {
                    for (int dy = 1; dy <= 8 && dropped < maxToDrop; dy++) { // check up to 8 blocks above the mob
                        var pos = mob.blockPosition().offset(dx, dy, dz);
                        var state = mob.level().getBlockState(pos);
                        // only stalactites
                        if (state.is(Blocks.POINTED_DRIPSTONE) && isStalactite(state)) {
                            BlockState above = mob.level().getBlockState(pos.above());
                            // "base" of the stalactite: block is attached to the ceiling (above is non-air)
                            // and the block above is NOT another pointed dripstone
                            if (!above.isAir() && !above.is(Blocks.POINTED_DRIPSTONE)) {
                                spawnFallingStalactite(state, (ServerLevel) mob.level(), pos);
                                dropped++;
                            }
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
