package com.jeremyseq.inhabitants.compat.jei;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.items.ModItems;
import com.jeremyseq.inhabitants.recipe.BogreRecipeManager;
import com.jeremyseq.inhabitants.recipe.CookingRecipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.recipe.RecipeType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@JeiPlugin
public class InhabitantsJEIPlugin implements IModPlugin {
    public static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "jei_plugin");

    public static final RecipeType<CookingRecipe> BOGRE_COOKING =
            RecipeType.create(Inhabitants.MODID, "cooking", CookingRecipe.class);

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
            new CookingRecipeCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(BOGRE_COOKING, BogreRecipeManager.getAllCookingRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.BOGRE_CAULDRON.get()), BOGRE_COOKING);
    }
}
