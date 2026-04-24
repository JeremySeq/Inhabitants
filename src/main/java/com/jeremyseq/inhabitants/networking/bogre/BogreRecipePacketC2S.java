package com.jeremyseq.inhabitants.networking.bogre;

import com.jeremyseq.inhabitants.recipe.CookingRecipe;
import com.jeremyseq.inhabitants.recipe.BogreRecipeManager;
import com.jeremyseq.inhabitants.gui.cauldron.CauldronMenu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.Slot;

import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class BogreRecipePacketC2S {

    private final int recipeIndex;
    private final boolean isShiftDown;

    public BogreRecipePacketC2S(int recipeIndex) {
        this(recipeIndex, false);
    }

    public BogreRecipePacketC2S(int recipeIndex, boolean isShiftDown) {
        this.recipeIndex = recipeIndex;
        this.isShiftDown = isShiftDown;
    }

    public static void encode(BogreRecipePacketC2S packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.recipeIndex);
        buf.writeBoolean(packet.isShiftDown);
    }

    public static BogreRecipePacketC2S decode(FriendlyByteBuf buf) {
        return new BogreRecipePacketC2S(buf.readInt(), buf.readBoolean());
    }

    public static void handle(BogreRecipePacketC2S packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            
            if (player == null || !(player.containerMenu instanceof CauldronMenu menu)) return;
            
            List<CookingRecipe> allRecipes = BogreRecipeManager.getAllCookingRecipes();
            if (packet.recipeIndex < 0 || packet.recipeIndex >= allRecipes.size()) return;
            CookingRecipe recipe = allRecipes.get(packet.recipeIndex);
            
            menu.clearCraftingContent();
            
            int maxFills = packet.isShiftDown ? 16 : 1;
            for (int f = 0; f < maxFills; f++) {
                boolean success = true;
                
                // ingredients 0-3
                for (int i = 0; i < recipe.ingredients().size(); i++) {
                    if (recipe.hasTagIngredient(i)) {

                        if (!findAndMoveTagItem(player, menu, recipe.getTagForSlot(i), i, recipe)) {
                            success = false;
                            break;
                        }
                    } else {
                        Item needed = recipe.ingredients().get(i);

                        if (needed != Items.AIR) {
                            if (!findAndMoveItem(player, menu, needed, i)) {
                                success = false;
                                break;
                            }
                        }
                    }
                }
                
                // container slot 4
                if (success) {
                    if (!findAndMoveItem(player, menu, recipe.container(), 4)) {
                        success = false;
                    }
                }
                
                if (!success) break;
            }
        });
        
        context.setPacketHandled(true);
    }

    private static boolean findAndMoveItem(ServerPlayer player, CauldronMenu menu, Item needed, int slotIndex) {
        Slot slot = menu.getSlot(slotIndex);
        if (slot.getItem().getCount() >= slot.getMaxStackSize()) return true;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (!stack.isEmpty() && stack.getItem() == needed) {
                ItemStack moved = player.getInventory().removeItem(i, 1);
                
                if (slot.hasItem()) {
                    slot.getItem().grow(1);
                    slot.setChanged();
                } else {
                    slot.set(moved);
                }

                return true;
            }
        }

        return false;
    }

    private static boolean findAndMoveTagItem(ServerPlayer player, CauldronMenu menu,
        TagKey<Item> tag, int slotIndex, CookingRecipe recipe) {
        
        Slot slot = menu.getSlot(slotIndex);
        if (slot.getItem().getCount() >= slot.getMaxStackSize()) return true;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (!stack.isEmpty() &&
                stack.is(tag) &&
                !recipe.isForbiddenCookedIngredient(stack.getItem())) {
                
                ItemStack moved = player.getInventory().removeItem(i, 1);

                if (slot.hasItem()) {
                    slot.getItem().grow(1);
                    slot.setChanged();
                } else {
                    slot.set(moved);
                }

                return true;
            }
        }
        
        return false;
    }
}


