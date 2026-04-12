package com.jeremyseq.inhabitants.recipe;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public record TransformationRecipe(
        Item ingredient,
        ItemStack result,
        int hammer_hits,
        Optional<SoundEvent> hammerSound
) implements IBogreRecipe {
    @Override
    public Type getBogreRecipeType() {
        return Type.TRANSFORMATION;
    }
}