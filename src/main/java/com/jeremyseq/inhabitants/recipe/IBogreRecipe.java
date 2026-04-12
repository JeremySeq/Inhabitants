package com.jeremyseq.inhabitants.recipe;

import net.minecraft.world.item.ItemStack;

/**
 * interface for all Bogre recipes: Cooking, Carving & Transformation
 */
public interface IBogreRecipe {
    ItemStack result();
    enum Type { COOKING, CARVING, TRANSFORMATION }
    Type getBogreRecipeType();
}
