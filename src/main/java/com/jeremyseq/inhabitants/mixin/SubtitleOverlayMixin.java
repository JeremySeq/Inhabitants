package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.effects.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin to obscure all subtitle texts when the player has the concussion effect.
 */
@Mixin(SubtitleOverlay.class)
public abstract class SubtitleOverlayMixin {

    /**
     * replaces the rendered subtitle text with inaudible without modifying the stored subtitle text (prevents spam)
     */
    @ModifyVariable(
            method = "render",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/client/gui/components/SubtitleOverlay$Subtitle;getText()Lnet/minecraft/network/chat/Component;"
            ),
            ordinal = 0
    )
    private Component inhabitants$modifyRenderedSubtitle(Component original) {

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return original;

        MobEffectInstance effect = mc.player.getEffect(ModEffects.CONCUSSION.get());
        if (effect == null) return original;

        return Component.translatable("subtitles.inhabitants.inaudible");
    }
}