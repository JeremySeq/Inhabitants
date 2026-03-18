package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.entities.bogre.ai.ShockwaveGoal;
import com.jeremyseq.inhabitants.items.GiantBoneItem;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class GiantBoneSpecialAttackMixin {

    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V",
    at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;crit(Lnet/minecraft/world/entity/Entity;)V"
    ),
    cancellable = true
    )
    private void inhabitants$giantBoneSpecialAttack(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        ItemStack handStack = player.getMainHandItem();

        if (handStack.getItem() instanceof GiantBoneItem) {
            if (player.getCooldowns().isOnCooldown(handStack.getItem())) return;

            float radius = GiantBoneItem.getShockwaveRadius();
            float damage = GiantBoneItem.getShockwaveDamage();
            int duration = GiantBoneItem.getShockwaveDuration();
            int cooldown = GiantBoneItem.getCooldown();
            float chance = 0.25F;

            if (player.getRandom().nextFloat() < chance) {
                if (!player.level().isClientSide) {
                    ShockwaveGoal.addShockwave(
                            (ServerLevel) player.level(),
                            target.position(),
                            damage,
                            radius,
                            duration,
                            player
                    );
                }
                
                player.getCooldowns().addCooldown(handStack.getItem(), cooldown);
                ci.cancel();
            }
        }
    }
}
