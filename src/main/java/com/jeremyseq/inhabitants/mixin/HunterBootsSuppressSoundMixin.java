package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.items.HunterBootsItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class HunterBootsSuppressSoundMixin {
    // Suppress step sounds for entities wearing Hunter Boots
    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void onPlayStepSound(BlockPos pos, BlockState state, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity livingEntity) {
            if (livingEntity.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof HunterBootsItem) {
                ci.cancel();
            }
        }
    }
}
