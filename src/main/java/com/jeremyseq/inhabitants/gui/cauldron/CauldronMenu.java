package com.jeremyseq.inhabitants.gui.cauldron;

import com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron.BogreCauldronEntity;
import com.jeremyseq.inhabitants.recipe.BogreRecipeManager;
import com.jeremyseq.inhabitants.gui.ModMenuTypes;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import org.jetbrains.annotations.NotNull;

import java.util.*;


public class CauldronMenu extends RecipeBookMenu<Container> {

    private static final int INGREDIENT_SLOTS = 4; // 4 ingredients slots 0, 1, 2, 3
    private static final int CONTAINER_SLOT = 4;
    private static final int CAULDRON_SLOTS = 5; // 4 ingredients + 1 container/output

    private final Level level;
    public final Player player;
    private final IItemHandler itemHandler;
    public BlockPos blockPosition;
    
    public CauldronMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, new ItemStackHandler(CAULDRON_SLOTS),
                extraData != null ? extraData.readBlockPos() : BlockPos.ZERO);
    }
    
    public CauldronMenu(int id, Inventory inventory, IItemHandler itemHandler, BlockPos blockPosition) {
        super(ModMenuTypes.CAULDRON_MENU.get(), id);
        this.player = inventory.player;
        this.level = inventory.player.level();
        this.itemHandler = itemHandler;
        this.blockPosition = blockPosition;

        // ingredient slots (0, 1, 2, 3), max 1 item each
        // container slot 4, max 1 item
        // slot 0
        this.addSlot(new SlotItemHandler(itemHandler, 0, 44, 19) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) {
                return isIngredient(stack);
            }
            @Override public void set(@NotNull ItemStack stack) {
                if (!isIngredient(stack)) {
                    BogreCauldronEntity cauldron = getCauldronEntity();
                    if (cauldron != null) cauldron.notifyInvalidIngredient(player);
                }
                super.set(stack);
            }
            @Override public int getMaxStackSize() { return 16; }
        });
        // slot 1
        this.addSlot(new SlotItemHandler(itemHandler, 1, 62, 19) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) {
                return isIngredient(stack);
            }
            @Override public void set(@NotNull ItemStack stack) {
                if (!isIngredient(stack)) {
                    BogreCauldronEntity cauldron = getCauldronEntity();
                    if (cauldron != null) cauldron.notifyInvalidIngredient(player);
                }
                super.set(stack);
            }
            @Override public int getMaxStackSize() { return 16; }
        });
        // slot 2
        this.addSlot(new SlotItemHandler(itemHandler, 2, 44, 37) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) {
                return isIngredient(stack);
            }
            @Override public void set(@NotNull ItemStack stack) {
                if (!isIngredient(stack)) {
                    BogreCauldronEntity cauldron = getCauldronEntity();
                    if (cauldron != null) cauldron.notifyInvalidIngredient(player);
                }
                super.set(stack);
            }
            @Override public int getMaxStackSize() { return 16; }
        });
        // slot 3
        this.addSlot(new SlotItemHandler(itemHandler, 3, 62, 37) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) {
                return isIngredient(stack);
            }
            @Override public void set(@NotNull ItemStack stack) {
                if (!isIngredient(stack)) {
                    BogreCauldronEntity cauldron = getCauldronEntity();
                    if (cauldron != null) cauldron.notifyInvalidIngredient(player);
                }
                super.set(stack);
            }
            @Override public int getMaxStackSize() { return 16; }
        });
        // slot 4 (bowl/output)
        this.addSlot(new SlotItemHandler(itemHandler, CONTAINER_SLOT, 98, 28) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) {
                return BogreRecipeManager.isContainer(stack.getItem());
            }
            @Override public int getMaxStackSize() { return 16; }
        });
        // player inventory slots
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inventory, col + (row + 1) * 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // player hotbar slots
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return player.distanceToSqr(
            blockPosition.getX() + 0.5D,
            blockPosition.getY() + 0.5D,
            blockPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        // ehm ehm... Bogre only gets angry if a slot that had items is now empty :D
        boolean wasEmpty = true;
        if (slotId >= 0 && slotId < CAULDRON_SLOTS) {
            wasEmpty = this.getSlot(slotId).getItem().isEmpty();
        }

        super.clicked(slotId, button, clickType, player);

        if (slotId >= 0 && slotId < CAULDRON_SLOTS) {
            boolean isEmptyNow = this.getSlot(slotId).getItem().isEmpty();
            if (!wasEmpty && isEmptyNow) {
                BogreCauldronEntity cauldron = getCauldronEntity();
                if (cauldron != null) {
                    cauldron.notifyTheft(player);
                }
            }
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player playerIn, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (!slot.hasItem()) return original;

        ItemStack slotItem = slot.getItem();
        original = slotItem.copy();

        if (index < CAULDRON_SLOTS) {
            // cauldron to player
            if (!this.moveItemStackTo(slotItem, CAULDRON_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(slotItem, original);
        } else {
            // player to cauldron
            boolean moved = false;
            if (BogreRecipeManager.isContainer(slotItem.getItem())) {
                moved = this.moveItemStackTo(slotItem, CONTAINER_SLOT, CONTAINER_SLOT + 1, false);
            } else if (isIngredient(slotItem)) {
                moved = this.moveItemStackTo(slotItem, 0, INGREDIENT_SLOTS, false);
            }
            
            if (!moved) {
                if (index < CAULDRON_SLOTS + 27) {
                    if (!this.moveItemStackTo(slotItem, CAULDRON_SLOTS + 27, this.slots.size(), true))
                        return ItemStack.EMPTY;
                } else {
                    if (!this.moveItemStackTo(slotItem, CAULDRON_SLOTS, CAULDRON_SLOTS + 27, false))
                        return ItemStack.EMPTY;
                }
            }
        }

        if (slotItem.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (slotItem.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(playerIn, slotItem);

        return original;
    }

    public int getCookingProgress() {
        BogreCauldronEntity entity = getCauldronEntity();
        return entity != null ? entity.getEntityData().get(BogreCauldronEntity.COOKING_PROGRESS) : 0;
    }

    public int getItemCount() {
        int count = 0;
        for (int i = 0; i < INGREDIENT_SLOTS; i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public boolean hasHeatSource() {
        BogreCauldronEntity entity = getCauldronEntity();
        return entity != null && entity.hasHeatSource();
    }

    @Override
    public void fillCraftSlotsStackedContents(@NotNull StackedContents helper) {
        // not used
    }
    
    @Override
    public void clearCraftingContent() {
        for (int i = 0; i < CAULDRON_SLOTS; i++) {
            ItemStack stack = this.itemHandler.extractItem(i, 64, false);
            if (!stack.isEmpty()) {
                if (!this.player.getInventory().add(stack)) {
                    this.player.drop(stack, false);
                }
            }
        }
    }

    @Override
    public boolean recipeMatches(@NotNull Recipe<? super Container> recipe) {
        return false;
    }

    @Override
    public @NotNull RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    public boolean shouldMoveToInventory(int slotIndex) {
        return slotIndex != this.getResultSlotIndex();
    }

    private BogreCauldronEntity getCauldronEntity() {
        List<BogreCauldronEntity> entities = level.getEntitiesOfClass(
            BogreCauldronEntity.class, new AABB(blockPosition).inflate(1.5));
        return entities.isEmpty() ? null : entities.get(0);
    }

    @Override public int getResultSlotIndex() { return CONTAINER_SLOT; }
    @Override public int getGridWidth() { return 2; }
    @Override public int getGridHeight() { return 2; }
    @Override public int getSize() { return CAULDRON_SLOTS; }
    
    private boolean isIngredient(ItemStack stack) {
        return BogreRecipeManager.isCookingIngredient(stack.getItem()) || (!stack.isEmpty() && stack.getItem().isEdible());
    }
}
