package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.effects.ModEffects;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class NightmarePanicPlayerMixin {
    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/tutorial/Tutorial;onInput(Lnet/minecraft/client/player/Input;)V", shift = At.Shift.AFTER))
    private void inhabitants$reverseInputs(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object)this;
        if (player.hasEffect(ModEffects.PANIC.get())) {
            player.input.leftImpulse *= -1;
            player.input.forwardImpulse *= -1;
        }
    }
}
