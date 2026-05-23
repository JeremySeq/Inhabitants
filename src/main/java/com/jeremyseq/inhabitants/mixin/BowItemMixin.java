package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(BowItem.class)
public abstract class BowItemMixin {
    @Inject(method = "getAllSupportedProjectiles", at = @At("HEAD"), cancellable = true)
    private void inhabitants$getAllSupportedProjectiles(CallbackInfoReturnable<Predicate<ItemStack>> cir) {
        cir.setReturnValue(ProjectileWeaponItem.ARROW_ONLY.or((item) -> item.is(ModItems.CONCUSSION_ARROW.get())));
    }
}
