package com.jeremyseq.inhabitants.entities.bogre.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.sounds.SoundEvent;
import java.util.Optional;

/**
 * Represents a recipe for the Bogre cauldron.
 * 
 */
public record BogreRecipe(
    Type type,
    Optional<Item> triggerItem,
    Optional<Block> triggerBlock,
    ItemStack result,
    float suspiciousStewChance,
    int requiredBlocks,
    Optional<SoundEvent> hammerSound
) {
    public enum Type {
        COOKING,
        CARVING,
        TRANSFORMATION
    }
}
