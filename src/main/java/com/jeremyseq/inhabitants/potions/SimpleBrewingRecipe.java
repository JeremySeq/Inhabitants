package com.jeremyseq.inhabitants.potions;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import org.jetbrains.annotations.NotNull;

public class SimpleBrewingRecipe implements IBrewingRecipe {
    private final Item inputBottle;
    private final Potion inputPotion;
    private final Item reagent;
    private final Potion outputPotion;

    public SimpleBrewingRecipe(Item inputBottle, Potion inputPotion, Item reagent, Potion outputPotion) {
        this.inputBottle = inputBottle;
        this.inputPotion = inputPotion;
        this.reagent = reagent;
        this.outputPotion = outputPotion;
    }

    @Override
    public boolean isInput(ItemStack input) {
        return input.getItem() == inputBottle && PotionUtils.getPotion(input) == inputPotion;
    }

    @Override
    public boolean isIngredient(ItemStack ingredient) {
        return ingredient.getItem() == reagent;
    }

    @Override
    public @NotNull ItemStack getOutput(@NotNull ItemStack input, @NotNull ItemStack ingredient) {
        if (isInput(input) && isIngredient(ingredient)) {
            return PotionUtils.setPotion(new ItemStack(inputBottle), outputPotion);
        }
        return ItemStack.EMPTY;
    }
}