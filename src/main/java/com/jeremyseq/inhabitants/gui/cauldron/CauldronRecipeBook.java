package com.jeremyseq.inhabitants.gui.cauldron;

import com.jeremyseq.inhabitants.gui.components.GuiLayout;
import com.jeremyseq.inhabitants.recipe.CookingRecipe;
import com.jeremyseq.inhabitants.recipe.BogreRecipeManager;
import com.jeremyseq.inhabitants.networking.bogre.BogreRecipePacketC2S;
import com.jeremyseq.inhabitants.networking.ModNetworking;
import com.jeremyseq.inhabitants.gui.components.ModGuiTextures;
import com.jeremyseq.inhabitants.gui.components.RenderUtil;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.jetbrains.annotations.NotNull;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.*;


public class CauldronRecipeBook extends AbstractWidget {
    
    private final int width = 147;
    private final int height = 166;
    private int parentHeight;
    private boolean widthTooNarrow;
    private int currentPage = 0;

    private static final int gridCols = 5;
    private static final int gridRows = 4;
    private static final int itemsPerPage = gridCols * gridRows;
    private static final int slotSize = 25;
    private static final int gridOffsetX = 11;
    private static final int gridOffsetY = 31;
    private static final int recipeBookGap = 4;

    private List<CookingRecipe> allRecipes = new ArrayList<>();
    private List<CookingRecipe> filteredRecipes = new ArrayList<>();

    private EditBox searchBox;

    private static boolean onlyCraftable = false;
    private static boolean visible = false;
    public static CookingRecipe activeGhostRecipe = null;

    private Minecraft minecraft;
    private GuiLayout bookLayout;

    public CauldronRecipeBook() {
        super(0, 0, 147, 166, Component.empty());
        this.minecraft = Minecraft.getInstance();
    }

    public void init(int parentWidth, int parentHeight, Minecraft minecraft,
        boolean widthTooNarrow, CauldronMenu menu) {
        
        this.parentHeight = parentHeight;
        this.minecraft = minecraft;
        this.widthTooNarrow = widthTooNarrow;

        this.allRecipes = BogreRecipeManager.getAllCookingRecipes();
        this.updateFilteredRecipes();
        this.updatePosition(widthTooNarrow, parentWidth, 176);

        GuiLayout searchBounds = bookLayout.createChild()
            .width("79").height("9").margin(14, 0, 0, 26).align("left", "top");

        this.searchBox = new EditBox(minecraft.font,
            searchBounds.x, searchBounds.y,
            searchBounds.width, searchBounds.height,
            RenderUtil.getLabel("itemGroup.search"));

        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(false);
        this.searchBox.setVisible(true);
        this.searchBox.setTextColor(0xFFFFFF);
        this.searchBox.setResponder(text -> this.updateFilteredRecipes());
    }

    public void updatePosition(boolean widthTooNarrow, int parentWidth, int guiWidth) {
        this.widthTooNarrow = widthTooNarrow;
        this.setX(updateScreenPosition(parentWidth, guiWidth) - this.width - recipeBookGap);

        GuiLayout screenLayout = new GuiLayout(0, 0, parentWidth, this.parentHeight);
        GuiLayout centered = screenLayout.createChild().width(this.width).height(this.height).align(null, "center");
        this.setY(centered.y);

        this.bookLayout = new GuiLayout(this.getX(), this.getY(), this.width, this.height);

        if (this.searchBox != null) {
            GuiLayout searchBounds = bookLayout.createChild()
                .width("79").height("9").margin(14, 0, 0, 26).align("left", "top");

            this.searchBox.setPosition(searchBounds.x, searchBounds.y);
        }
    }

    public int updateScreenPosition(int parentWidth, int guiWidth) {
        if (this.isVisible() && !this.widthTooNarrow) {
            return 147 + recipeBookGap + (parentWidth - guiWidth - 147 - recipeBookGap) / 2;
        }
        return (parentWidth - guiWidth) / 2;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics,
        int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        RenderUtil.blit(guiGraphics, ModGuiTextures.RECIPE_BOOK_WIDGET, this.getX(), this.getY());

        renderTopSection(guiGraphics, mouseX, mouseY, partialTicks);
        renderCenterSection(guiGraphics, mouseX, mouseY);
        renderBottomSection(guiGraphics, mouseX, mouseY);
    }

    /**
    * search bar + filter button
    */
    private void renderTopSection(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // search bar
        ResourceLocation searchTex = this.searchBox.isFocused() ? ModGuiTextures.TEXT_FIELD_HIGHLIGHTED : ModGuiTextures.TEXT_FIELD;
        
        guiGraphics.blit(searchTex, this.getX() + 24, this.getY() + 10, 0, 0, 84, 16, 84, 16);
        this.searchBox.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (this.searchBox.getValue().isEmpty() &&
        !this.searchBox.isFocused()) {

            Component hint = RenderUtil.getHint("gui.recipebook.search_hint");

            guiGraphics.drawString(this.minecraft.font, hint, this.getX() + 28, this.getY() + 14, 0xFFFFFF, false);
        }

        // filter button
        GuiLayout filterBtn = bookLayout.createChild()
            .width("26").height("16").margin(10, 11, 0, 0).align("right", "top");

        boolean hovering = filterBtn.isHovered(mouseX, mouseY);
        ModGuiTextures.SpriteRegion filterTex;

        if (onlyCraftable) {
            filterTex = hovering ? ModGuiTextures.FILTER_ENABLED_HIGHLIGHTED : ModGuiTextures.FILTER_ENABLED;
        } else {
            filterTex = hovering ? ModGuiTextures.FILTER_DISABLED_HIGHLIGHTED : ModGuiTextures.FILTER_DISABLED;
        }

        RenderUtil.blit(guiGraphics, filterTex, filterBtn);
    }

    /**
    * item grid + hover tooltips
    */
    private void renderCenterSection(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int startIndex = currentPage * itemsPerPage;

        for (int i = 0; i < itemsPerPage && startIndex + i < filteredRecipes.size(); i++) {
            CookingRecipe recipe = filteredRecipes.get(startIndex + i);
            int[] pos = getGridSlotPosition(i);
            GuiLayout slotBounds = new GuiLayout(pos[0], pos[1], slotSize, slotSize);

            boolean craftable = canCraft(recipe);
            RenderUtil.blit(guiGraphics, craftable ? ModGuiTextures.SLOT_CRAFTABLE : ModGuiTextures.SLOT_UNCRAFTABLE, slotBounds);
            guiGraphics.renderItem(recipe.result(), pos[0] + 4, pos[1] + 4);
            
            if (slotBounds.isHovered(mouseX, mouseY)) {
                guiGraphics.fill(pos[0], pos[1], pos[0] + slotSize, pos[1] + slotSize, 0x30FFFFFF);
            }
        }
        
        for (int i = 0; i < itemsPerPage && startIndex + i < filteredRecipes.size(); i++) {
            int[] pos = getGridSlotPosition(i);
            GuiLayout slotBounds = new GuiLayout(pos[0], pos[1], slotSize, slotSize);
            if (slotBounds.isHovered(mouseX, mouseY)) {
                renderRecipeTooltip(guiGraphics, filteredRecipes.get(startIndex + i), mouseX, mouseY);
            }
        }
    }

    /**
    * mavigation arrows + page counter
    */
    private void renderBottomSection(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (filteredRecipes.size() <= itemsPerPage) return;

        GuiLayout backBtn = bookLayout.createChild()
            .width("12").height("17").margin(0, 0, 12, 38).align("left", "bottom");

        GuiLayout forwardBtn = bookLayout.createChild()
            .width("12").height("17").margin(0, 42, 12, 0).align("right", "bottom");

        // backward arrow
        if (currentPage > 0) {
            ModGuiTextures.SpriteRegion tex = backBtn.isHovered(mouseX, mouseY) ?
                ModGuiTextures.PAGE_BACKWARD_HIGHLIGHTED : ModGuiTextures.PAGE_BACKWARD;

            RenderUtil.blit(guiGraphics, tex, backBtn);
        }

        // forward arrow
        if (hasNextPage()) {
            ModGuiTextures.SpriteRegion tex = forwardBtn.isHovered(mouseX, mouseY) ?
                ModGuiTextures.PAGE_FORWARD_HIGHLIGHTED : ModGuiTextures.PAGE_FORWARD;
                
            RenderUtil.blit(guiGraphics, tex, forwardBtn);
        }

        // page counter
        int totalPages = (filteredRecipes.size() - 1) / itemsPerPage + 1;
        String pageText = (currentPage + 1) + "/" + totalPages;
        int textWidth = minecraft.font.width(pageText);

        GuiLayout pageCounter = bookLayout.createChild()
            .width(textWidth).height("9").margin(0, 0, 16, 0).align("center", "bottom");

        guiGraphics.drawString(minecraft.font, pageText, pageCounter.x, pageCounter.y, -1, false);
    }

    private void renderRecipeTooltip(GuiGraphics guiGraphics,
        CookingRecipe recipe, int mouseX, int mouseY) {
        
        Component title = RenderUtil.styleTooltipTitle(recipe.result().getHoverName());
        int titleWidth = minecraft.font.width(title);
        
        int numIngredients = recipe.ingredients().size();
        int slotSize = 18;
        int spacing = 2;
        int arrowWidth = 12;

        int slotsWidth = (2 * slotSize) + spacing +
            (spacing * 4) + (arrowWidth * 2) + (slotSize * 2);
        
        GuiLayout tooltipLayout = GuiLayout.screen(this.minecraft)
            .width(Math.max(titleWidth, slotsWidth) + 8)
            .height(9 + 4 + (2 * slotSize + spacing) + 4)
            .offset(mouseX + 12, mouseY - 12)
            .clampToScreen(this.minecraft);
        
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 400);
        
        tooltipLayout.zIndex(0);
        RenderUtil.renderTooltipBackground(guiGraphics, tooltipLayout);

        // content area with padding
        GuiLayout content = tooltipLayout.createChild().padding(4);

        // title text
        guiGraphics.drawString(minecraft.font, title, content.x, content.y, -1, true);
        
        // slots
        int currentX = content.x;
        int slotsY = content.y + 13;
        long time = System.currentTimeMillis() / 50;
        
        // ingredient slots
        for (int i = 0; i < 4; i++) {
            RenderSystem.enableBlend();
            
            int col = i % 2;
            int row = i / 2;
            int slotX = currentX + (col * (slotSize + spacing));
            int slotY = slotsY + (row * (slotSize + spacing));
            
            guiGraphics.blit(ModGuiTextures.SLOT, slotX, slotY, 0, 0,
                slotSize, slotSize, slotSize, slotSize);
            
            if (i < numIngredients) {
                Item displayItem = recipe.getDisplayItem(i, time);
                if (displayItem != Items.AIR) {
                    guiGraphics.renderItem(new ItemStack(displayItem), slotX + 1, slotY + 1);
                }
            }
        }
        
        currentX += (2 * slotSize) + spacing + spacing;
        int gridHeight = (2 * slotSize) + spacing;
        int centerY = slotsY + gridHeight / 2;
        
        // first arrow
        guiGraphics.drawString(minecraft.font,
            Component.literal("→")
            .withStyle(ChatFormatting.GRAY),
            currentX + 1, centerY - 4,
            -1, true);

        currentX += arrowWidth + spacing;
        
        // container slot
        guiGraphics.blit(ModGuiTextures.SLOT, currentX, centerY - 9, 0, 0,
            slotSize, slotSize, slotSize, slotSize);
        guiGraphics.renderItem(new ItemStack(recipe.container()), currentX + 1, centerY - 8);

        currentX += slotSize + spacing + spacing;

        // second arrow, container → output
        guiGraphics.drawString(minecraft.font,
            Component.literal("→")
            .withStyle(ChatFormatting.GRAY),
            currentX + 1, centerY - 4,
            -1, true);

        currentX += arrowWidth + spacing;

        // output slot
        RenderSystem.enableBlend();
        guiGraphics.blit(ModGuiTextures.SLOT, currentX, centerY - 9, 0, 0,
            slotSize, slotSize, slotSize, slotSize);
        guiGraphics.renderItem(recipe.result(), currentX + 1, centerY - 8);
        guiGraphics.renderItemDecorations(minecraft.font, recipe.result(), currentX + 1, centerY - 8);
        
        guiGraphics.pose().popPose();
    }

    private void updateFilteredRecipes() {
        String search = this.searchBox != null ? this.searchBox.getValue().toLowerCase() : "";
        this.filteredRecipes = allRecipes.stream()
            .filter(r -> r.result().getHoverName().getString().toLowerCase().contains(search))
            .filter(r -> !onlyCraftable || canCraft(r))
            .toList();
        this.currentPage = 0;
    }
    
    private boolean canCraft(@NotNull CookingRecipe recipe) {
        if(minecraft.player == null) return false;
        Inventory inv = minecraft.player.getInventory();
        Map<Item, Integer> exactNeeded = new HashMap<>();
        Map<TagKey<Item>, Integer> tagsNeeded = new HashMap<>();
        
        for (int i = 0; i < recipe.ingredients().size(); i++) {
            if (recipe.hasTagIngredient(i)) {
                tagsNeeded.merge(recipe.getTagForSlot(i), 1, Integer::sum);
            } else {
                exactNeeded.merge(recipe.ingredients().get(i), 1, Integer::sum);
            }
        }

        exactNeeded.merge(recipe.container(), 1, Integer::sum);
        
        boolean[] usedItems = new boolean[inv.getContainerSize()];
        
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && exactNeeded.containsKey(stack.getItem())) {
                int neededCount = exactNeeded.get(stack.getItem());
                int stackCount = stack.getCount();
                
                if (neededCount <= stackCount) {
                    exactNeeded.remove(stack.getItem());
                    usedItems[i] = true;
                } else {
                    exactNeeded.put(stack.getItem(), neededCount - stackCount);
                    usedItems[i] = true;
                }
            }
        }

        if (!exactNeeded.isEmpty()) return false;
        
        for (var entry : tagsNeeded.entrySet()) {
            TagKey<Item> tag = entry.getKey();
            int needed = entry.getValue();

            for (int i = 0; i < inv.getContainerSize() && needed > 0; i++) {
                if (usedItems[i]) continue;
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && stack.is(tag) && !recipe.isForbiddenCookedIngredient(stack.getItem())) {
                    int consume = Math.min(needed, stack.getCount());
                    needed -= consume;
                    usedItems[i] = true;
                }
            }
            if (needed > 0) return false;
        }

        return true;
    }

    private void handleRecipeClick(@NotNull CookingRecipe recipe) {
        this.minecraft.getSoundManager()
            .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        
        activeGhostRecipe = canCraft(recipe) ? null : recipe;

        List<CookingRecipe> allRecipes = BogreRecipeManager.getAllCookingRecipes();
        int index = allRecipes.indexOf(recipe);

        if (index != -1) {
            ModNetworking.sendToServer(new BogreRecipePacketC2S(index));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        // search box
        if (this.searchBox.mouseClicked(mouseX, mouseY, button)) {
            this.searchBox.setFocused(true);
            return true;
        }

        this.searchBox.setFocused(false);
        
        GuiLayout filterBtn = bookLayout.createChild()
            .width("26").height("16").margin(10, 11, 0, 0).align("right", "top");

        GuiLayout backBtn = bookLayout.createChild()
            .width("12").height("17").margin(0, 0, 12, 38).align("left", "bottom");

        GuiLayout forwardBtn = bookLayout.createChild()
            .width("12").height("17").margin(0, 42, 12, 0).align("right", "bottom");

        // filter toggle
        if (filterBtn.isHovered(mouseX, mouseY)) {
            onlyCraftable = !onlyCraftable;

            this.minecraft.getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            
            updateFilteredRecipes();
            return true;
        }

        // navigation
        if (currentPage > 0 && backBtn.isHovered(mouseX, mouseY)) {
            currentPage--;

            this.minecraft.getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        if (hasNextPage() && forwardBtn.isHovered(mouseX, mouseY)) {
            currentPage++;
            this.minecraft.getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        
        int startIndex = currentPage * itemsPerPage;
        for (int i = 0; i < itemsPerPage && startIndex + i < filteredRecipes.size(); i++) {
            int[] pos = getGridSlotPosition(i);
            GuiLayout slotBounds = new GuiLayout(pos[0], pos[1], slotSize, slotSize);
            if (slotBounds.isHovered(mouseX, mouseY)) {
                handleRecipeClick(filteredRecipes.get(startIndex + i));
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (visible && this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (visible && this.searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private int[] getGridSlotPosition(int index) {
        int col = index % gridCols;
        int row = index / gridCols;
        return new int[]{
                this.getX() + gridOffsetX + col * slotSize,
                this.getY() + gridOffsetY + row * slotSize
        };
    }

    private boolean hasNextPage() {
        return (currentPage + 1) * itemsPerPage < filteredRecipes.size();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE,
        RenderUtil.getLabel("gui.recipebook.title"));
    }

    public void toggleVisibility() {
        visible = !visible;
    }

    public boolean isVisible() {
        return visible;
    }
}
