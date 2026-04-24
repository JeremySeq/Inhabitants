package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.effects.ModEffects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void inhabitants$immaterialClimb(Vec3 pTravelVector, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player player &&
                ModEffects.IMMATERIAL.isPresent() &&
                player.hasEffect(ModEffects.IMMATERIAL.get())) {

            if (inhabitants$isInsideBlock(player)) {
                Vec3 movement = player.getDeltaMovement();
                boolean isJumping = ((LivingEntityAccessor) player).inhabitants$isJumping();
                boolean isSneaking = player.isCrouching();

                double verticalSpeed = 0.0;
                if (isJumping) {
                    verticalSpeed = 0.2;
                } else if (isSneaking) {
                    verticalSpeed = -0.2;
                }

                if (verticalSpeed != 0.0) {
                    player.setDeltaMovement(
                            movement.x,
                            movement.y * 0.5 + verticalSpeed * 0.5,
                            movement.z
                    );
                    player.fallDistance = 0;
                } else {
                    player.setDeltaMovement(
                            movement.x,
                            movement.y * 0.8,
                            movement.z
                    );
                }
            }
        }
    }

    @Unique
    private boolean inhabitants$isInsideBlock(Player player) {
        Level level = player.level();
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        for (double dy : new double[]{0.1D, 0.8D, 1.62D}) {
            BlockPos pos = BlockPos.containing(px, py + dy, pz);
            if (!level.getBlockState(pos).getCollisionShape(
                    level, pos, CollisionContext.empty()).isEmpty()
            ) return true;
        }
        return false;
    }
}