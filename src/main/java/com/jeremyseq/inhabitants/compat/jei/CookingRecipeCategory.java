package com.jeremyseq.inhabitants.compat.jei;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.recipe.CookingRecipe;
import com.jeremyseq.inhabitants.gui.components.ModGuiTextures;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;

import org.jetbrains.annotations.NotNull;

public class CookingRecipeCategory implements IRecipeCategory<CookingRecipe> {
    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    public CookingRecipeCategory(IGuiHelper helper) {
        this.title = Component.translatable("gui.inhabitants.jei.cooking");
        this.background = helper.createBlankDrawable(150, 50);
        this.icon = helper.drawableBuilder(
            ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/item/bogre_cauldron.png"), 
            0, 0, 16, 16)
            .setTextureSize(16, 16)
            .build();
    }

    @Override
    public @NotNull RecipeType<CookingRecipe> getRecipeType() {
        return InhabitantsJEIPlugin.BOGRE_COOKING;
    }

    @Override
    public @NotNull Component getTitle() {
        return title;
    }

    @Override
    public @NotNull IDrawable getBackground() {
        return background;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull CookingRecipe recipe, @NotNull IFocusGroup focuses) {
        int slotSize = 18;
        int spacing = 2;
        int startX = 15;
        int slotsY = 5;
        
        for (int i = 0; i < 4; i++) {
            int col = i % 2;
            int row = i / 2;
            int x = startX + (col * (slotSize + spacing));
            int y = slotsY + (row * (slotSize + spacing));

            if (i < recipe.ingredients().size()) {
                if (recipe.tagIngredients() != null && i < recipe.tagIngredients().size() && recipe.tagIngredients().get(i) != null) {
                    assert Minecraft.getInstance().level != null;
                    builder.addSlot(RecipeIngredientRole.INPUT, x + 1, y + 1)
                        .addIngredients(VanillaTypes.ITEM_STACK, 
                        Minecraft.getInstance().level.registryAccess()
                        .registryOrThrow(Registries.ITEM)
                        .getTag(recipe.tagIngredients().get(i))
                        .get()
                        .stream()
                        .map(net.minecraft.core.Holder::value)
                        .filter(item -> !recipe.isForbiddenCookedIngredient(item))
                        .map(ItemStack::new)
                        .toList());
                } else {
                    builder.addSlot(RecipeIngredientRole.INPUT, x + 1, y + 1)
                        .addItemStack(new ItemStack(recipe.ingredients().get(i)));
                }
            }
        }

        int currentX = startX + (2 * slotSize) + spacing + 18;
        
        builder.addSlot(RecipeIngredientRole.INPUT, currentX + 1, slotsY + 10)
                .addItemStack(new ItemStack(recipe.container()));

        currentX += slotSize + spacing + 18 + spacing;
        
        builder.addSlot(RecipeIngredientRole.OUTPUT, currentX + 1, slotsY + 10)
                .addItemStack(recipe.result());
    }

    @Override
    public void draw(
            @NotNull CookingRecipe recipe,
            @NotNull IRecipeSlotsView recipeSlotsView,
            @NotNull GuiGraphics guiGraphics,
            double mouseX,
            double mouseY) {
        
        int slotSize = 18;
        int spacing = 2;
        int startX = 15;
        int slotsY = 5;

        Font font = Minecraft.getInstance().font;

        for (int i = 0; i < 4; i++) {
            int col = i % 2;
            int row = i / 2;

            guiGraphics.blit(
                ModGuiTextures.SLOT,
                startX + (col * (slotSize + spacing)), 
                slotsY + (row * (slotSize + spacing)), 
                0, 0, slotSize, slotSize, slotSize, slotSize);
        }

        int currentX = startX + (2 * slotSize) + spacing;
        int gridHeight = (2 * slotSize) + spacing;
        int centerY = slotsY + gridHeight / 2;

        guiGraphics.drawString(
            font, 
            Component.literal("→").withStyle(ChatFormatting.GRAY), 
            currentX + 6, 
            centerY - 4, 
            0x808080, 
            true);

        currentX += 16 + spacing;
        guiGraphics.blit(
            ModGuiTextures.SLOT, 
            currentX, centerY - 9, 
            0, 0, 
            slotSize, slotSize, 
            slotSize, slotSize);

        currentX += slotSize + spacing + spacing;

        guiGraphics.drawString(
            font, 
            Component.literal("→").withStyle(ChatFormatting.GRAY), 
            currentX + 6, 
            centerY - 4,
            0x808080, 
            true);

        currentX += 16 + spacing;
        guiGraphics.blit(
            ModGuiTextures.SLOT, 
            currentX, centerY - 9, 
            0, 0, 
            slotSize, slotSize, 
            slotSize, slotSize);


        Component timeText = Component.literal(recipe.time_ticks() / 20 + "s").withStyle(ChatFormatting.GRAY);

        guiGraphics.drawString(
            font, 
            timeText, 
            background.getWidth() - 25, 
            40, 
            0x808080, 
            false);
    }
}
