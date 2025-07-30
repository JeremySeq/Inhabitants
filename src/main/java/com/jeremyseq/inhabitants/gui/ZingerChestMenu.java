package com.jeremyseq.inhabitants.gui;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ZingerChestMenu extends AbstractContainerMenu {
    private final Container chest;

    public ZingerChestMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(27));
    }

    public ZingerChestMenu(int id, Inventory playerInv, Container chest) {
        super(ModMenuTypes.ZINGER_CHEST.get(), id);
        this.chest = chest;

        checkContainerSize(chest, 27);
        chest.startOpen(playerInv.player);

        // Zinger chest inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(chest, j + i * 9, 8 + j * 18, 18 + i * 18));
            }
        }

        // Player inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // Hotbar
        for (int j = 0; j < 9; ++j) {
            this.addSlot(new Slot(playerInv, j, 8 + j * 18, 142));
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.chest.stillValid(player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            originalStack = stackInSlot.copy();

            int containerSize = this.chest.getContainerSize();
            int fullSize = this.slots.size();

            if (index < containerSize) {
                // from Zinger chest -> to player inventory
                if (!this.moveItemStackTo(stackInSlot, containerSize, fullSize, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // from player inventory -> to Zinger chest
                if (!this.moveItemStackTo(stackInSlot, 0, containerSize, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return originalStack;
    }
}

