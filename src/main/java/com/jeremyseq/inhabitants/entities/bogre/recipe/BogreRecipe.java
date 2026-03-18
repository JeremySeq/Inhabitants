package com.jeremyseq.inhabitants.entities.bogre.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;

import java.util.*;

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
    List<StewEffect> stewEffects,
    int requiredBlocks,
    Optional<SoundEvent> hammerSound,
    int time_ticks,
    int hammer_hits
) {
    public enum Type {
        COOKING,
        CARVING,
        TRANSFORMATION
    }

    public record StewEffect(MobEffect effect, int duration) {}
}
