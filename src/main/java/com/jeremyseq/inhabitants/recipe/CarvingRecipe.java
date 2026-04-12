package com.jeremyseq.inhabitants.recipe;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

public record CarvingRecipe(
        Block triggerBlock,
        int requiredBlocks,
        ItemStack result,
        int hammer_hits,
        Optional<SoundEvent> hammerSound
) implements IBogreRecipe {
    @Override
    public Type getBogreRecipeType() {
        return Type.CARVING;
    }
}