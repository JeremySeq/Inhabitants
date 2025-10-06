package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID)
public class ChitinShieldEvents {
    /**
     * Deflects projectiles when the player is blocking with the chitin shield.
     */
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult().getType() == HitResult.Type.ENTITY) {
            EntityHitResult hit = (EntityHitResult) event.getRayTraceResult();
            Entity target = hit.getEntity();
            if (target instanceof Player player && player.isBlocking()) {
                ItemStack active = player.getUseItem();
                if (active.getItem() == ModItems.CHITIN_SHIELD.get()) {
                    Projectile projectile = event.getProjectile();

                    double speed = projectile.getDeltaMovement().length();

                    Vec3 look = player.getLookAngle().normalize();

                    Vec3 newMotion = look.scale(speed);
                    projectile.setDeltaMovement(newMotion);

                    float yaw = (float) (Mth.atan2(newMotion.x, newMotion.z) * (180F / Math.PI));
                    float pitch = (float) (Mth.atan2(newMotion.y, newMotion.horizontalDistance()) * (180F / Math.PI));
                    projectile.setYRot(yaw);
                    projectile.setXRot(pitch);
                    projectile.yRotO = yaw;
                    projectile.xRotO = pitch;
                    projectile.setOwner(player);

                    active.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));

                    event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);

                }
            }
        }
    }
}

