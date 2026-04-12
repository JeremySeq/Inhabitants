package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.blocks.impaler_head.ImpalerHeadBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoteBlock.class)
public class NoteBlockMixin {
    @Inject(
        method = "getCustomSoundId",
        at = @At("HEAD"),
        cancellable = true
    )
    private void inhabitants$getCustomSoundId(
        Level pLevel,
        BlockPos pPos,
        CallbackInfoReturnable<ResourceLocation> cir
    ) {
        BlockEntity blockentity = pLevel.getBlockEntity(pPos.above());

        if (blockentity instanceof ImpalerHeadBlockEntity head) {
            cir.setReturnValue(head.getNoteBlockSound());
        }
    }
}
