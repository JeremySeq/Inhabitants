package com.jeremyseq.inhabitants.gui.cauldron;

import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import com.jeremyseq.inhabitants.gui.components.GuiLayout;
import com.jeremyseq.inhabitants.gui.components.ModGuiTextures;
import com.jeremyseq.inhabitants.recipe.CookingRecipe;
import com.jeremyseq.inhabitants.gui.components.RenderUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.mojang.blaze3d.systems.RenderSystem;

import org.jetbrains.annotations.NotNull;

import org.lwjgl.glfw.GLFW;

public class CauldronScreen extends AbstractContainerScreen<CauldronMenu> {
    private final CauldronRecipeBook recipeBookComponent = new CauldronRecipeBook();
    private boolean widthTooNarrow;
    private boolean hasPlayedOpenSound = false;
    private ImageButton recipeButton;
    private GuiLayout bgLayout;
    
    public CauldronScreen(CauldronMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.widthTooNarrow = this.width < 335;

        assert this.minecraft != null;
        this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow, this.menu);
        this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;

        playOpenSound();
        createRecipeButton();

        this.addRenderableWidget(this.recipeButton);
        this.addRenderableWidget(this.recipeBookComponent);
    }

    private void playOpenSound() {
        if (!this.hasPlayedOpenSound) {
            Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(ModSoundEvents.CAULDRON_GUI_OPEN.get(), 1.0F, 1.0F));
            this.hasPlayedOpenSound = true;
        }
    }

    private void createRecipeButton() {
        int btnY = this.height / 2 - 49;
        this.recipeButton = new ImageButton(
            this.leftPos + 5, btnY, 20, 18, 0, 0, 19,
            ModGuiTextures.RECIPE_BUTTON, (button) -> {

            this.recipeBookComponent.toggleVisibility();
            this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
            this.recipeBookComponent.updatePosition(this.widthTooNarrow, this.width, this.imageWidth);
            this.repositionElements();

        });
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // who needs a default label when you're my label?
        // however... you can call me Bader ;)
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        renderHeatSourceTooltip(guiGraphics, mouseX, mouseY);
        renderGhostItems(guiGraphics);
        renderGhostItemTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        this.bgLayout = new GuiLayout(this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        RenderUtil.blit(guiGraphics, ModGuiTextures.CAULDRON_GUI, bgLayout);
        
        GuiLayout fireBounds = bgLayout.createChild()
            .width("16").height("16")
            .margin(27, 0, 0, 115)
            .align("left", "top");
            
        ResourceLocation fireTex = menu.hasHeatSource() ?
            ModGuiTextures.FIRE_SOURCE_ON : ModGuiTextures.FIRE_SOURCE_OFF;
        
        RenderUtil.blit(guiGraphics, fireTex, fireBounds);


        renderSlots(guiGraphics);
        renderCookingProgress(guiGraphics);
    }

    private void renderSlots(GuiGraphics guiGraphics) {
        CookingRecipe ghostRecipe = CauldronRecipeBook.activeGhostRecipe;

        // ingredient slots 0-3
        for (int i = 0; i < 4; i++) {
            Slot slot = menu.getSlot(i);

            boolean isGhost = !slot.hasItem() &&
                ghostRecipe != null && i < ghostRecipe.ingredients().size();

            GuiLayout slotLayout = bgLayout.createChild()
                .width(18).height(18).offset(slot.x - 1, slot.y - 1);
            
            RenderUtil.blit(guiGraphics,
                isGhost ? ModGuiTextures.GHOST_SLOT : ModGuiTextures.SLOT, slotLayout);
        }

        // container slot 4
        Slot containerSlot = menu.getSlot(4);
        boolean containerIsGhost = !containerSlot.hasItem() && ghostRecipe != null;
        
        GuiLayout containerLayout = bgLayout.createChild()
            .width(18).height(18).offset(containerSlot.x - 1, containerSlot.y - 1);
        
        ResourceLocation containerTex;
        if (containerIsGhost) {
            containerTex = ModGuiTextures.GHOST_SLOT;
        } else if (containerSlot.hasItem()) {
            containerTex = ModGuiTextures.SLOT;
        } else {
            containerTex = ModGuiTextures.BOWL_SLOT;
        }

        RenderUtil.blit(guiGraphics, containerTex, containerLayout);

    }

    private void renderGhostItems(GuiGraphics guiGraphics) {
        CookingRecipe ghostRecipe = CauldronRecipeBook.activeGhostRecipe;
        if (ghostRecipe == null) return;

        long time = System.currentTimeMillis() / 50;

        // ghost ingredients
        for (int i = 0; i < ghostRecipe.ingredients().size(); i++) {
            Slot slot = menu.getSlot(i);
            if (!slot.hasItem()) {
                Item displayItem = ghostRecipe.getDisplayItem(i, time);
                if (displayItem != Items.AIR) {
                    RenderUtil.renderGhostItem(guiGraphics, new ItemStack(displayItem),
                        this.leftPos + slot.x, this.topPos + slot.y);
                }
            }
        }

        // ghost container item
        Slot containerSlot = menu.getSlot(4);
        if (!containerSlot.hasItem()) {
            RenderUtil.renderGhostItem(guiGraphics, new ItemStack(ghostRecipe.container()),
                this.leftPos + containerSlot.x, this.topPos + containerSlot.y);
        }
    }

    private void renderGhostItemTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        CookingRecipe ghostRecipe = CauldronRecipeBook.activeGhostRecipe;
        if (ghostRecipe == null) return;

        long time = System.currentTimeMillis() / 50;

        // ghost ingredients
        for (int i = 0; i < ghostRecipe.ingredients().size(); i++) {
            Slot slot = menu.getSlot(i);
            if (!slot.hasItem() && this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                Item displayItem = ghostRecipe.getDisplayItem(i, time);
                guiGraphics.renderTooltip(this.font, new ItemStack(displayItem), mouseX, mouseY);
                return;
            }
        }

        // ghost container tooltip
        Slot containerSlot = menu.getSlot(4);
        if (!containerSlot.hasItem() && this.isHovering(containerSlot.x, containerSlot.y, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, new ItemStack(ghostRecipe.container()), mouseX, mouseY);
        }
    }

    private void renderCookingProgress(GuiGraphics guiGraphics) {
        int progress = menu.getCookingProgress();
        if (progress > 0) {
            int stage = (progress - 1) / 20;
            if (stage >= 0 && stage < 8) {
                GuiLayout bubbles = bgLayout.createChild()
                    .width("16").height("32").margin(20, 0, 0, 80).align("left", "top");

                RenderUtil.blit(guiGraphics, ModGuiTextures.BUBBLES[stage], bubbles);
            }
        }
        RenderSystem.disableBlend();
    }

    private void renderHeatSourceTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        GuiLayout fireBounds = bgLayout.createChild()
        .width("16").height("16").margin(27, 0, 0, 115).align("left", "top");
        
        if (!menu.hasHeatSource() && fireBounds.isHovered(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font,
                RenderUtil.getLabel("gui.inhabitants.missing_heat_source"),
                mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // clicking a ghost slot clears the ghost recipe
        if (CauldronRecipeBook.activeGhostRecipe != null) {
            for (int i = 0; i < 5; i++) { // 0-4 (ingredients + container)
                Slot slot = menu.getSlot(i);
                if (!slot.hasItem() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    CauldronRecipeBook.activeGhostRecipe = null;
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
        // if there's something strange, in your neighborhood,
        // who you gonna call?
        // GHOSTBUSTERS!
    }
}