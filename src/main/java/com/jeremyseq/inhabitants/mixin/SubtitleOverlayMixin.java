package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.effects.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to change all subtitle texts to "???" when the player has the concussion effect.
 */
@Mixin(SubtitleOverlay.Subtitle.class)
public abstract class SubtitleOverlayMixin {

    @Inject(method="getText", at=@At("HEAD"), cancellable = true)
    private void getText(CallbackInfoReturnable<Component> cir) {
        if (Minecraft.getInstance().player == null) return;
        MobEffectInstance concussion_effect = Minecraft.getInstance().player.getEffect(ModEffects.CONCUSSION.get());
        if (concussion_effect != null) {
            cir.setReturnValue(Component.literal("???"));
        }
    }
}
