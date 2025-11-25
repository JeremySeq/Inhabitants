package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.items.HunterBootsItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class HunterBootsSuppressGameEventMixin {
    // Suppress ground hit and step events for entities wearing Hunter Boots
    @Inject(method="gameEvent", at=@At("HEAD"), cancellable = true)
    private void onGameEvent(GameEvent pEvent, Vec3 pPos, GameEvent.Context pContext, CallbackInfo ci) {
        if (pEvent == GameEvent.HIT_GROUND || pEvent == GameEvent.STEP) {
            if (pContext.sourceEntity() instanceof LivingEntity entity) {
                if (entity.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof HunterBootsItem) {
                    ci.cancel();
                }
            }
        }

    }
}
