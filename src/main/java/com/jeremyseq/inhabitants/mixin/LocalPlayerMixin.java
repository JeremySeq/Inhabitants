package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.items.SpikeDrillItem;

import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Redirect(
        method = "aiStep()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"
        )
    )
    private boolean inhabitants$noInputSlowdownForDrill(LocalPlayer player) {
        if (player.getUseItem().getItem() instanceof SpikeDrillItem) {
            return false;
        }
        return player.isUsingItem();
    }
}
