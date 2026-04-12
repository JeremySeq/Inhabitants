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

import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class BogreRecipePacketC2S {

    private final int recipeIndex;

    public BogreRecipePacketC2S(int recipeIndex) {
        this.recipeIndex = recipeIndex;
    }

    public static void encode(BogreRecipePacketC2S packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.recipeIndex);
    }

    public static BogreRecipePacketC2S decode(FriendlyByteBuf buf) {
        return new BogreRecipePacketC2S(buf.readInt());
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
            
            boolean[] usedSlots = new boolean[player.getInventory().getContainerSize()];

            for (int i = 0; i < recipe.ingredients().size(); i++) {
                if (recipe.hasTagIngredient(i)) {
                    findAndMoveTagItem(player, menu, recipe.getTagForSlot(i), i, usedSlots, recipe);
                } else {
                    Item needed = recipe.ingredients().get(i);
                    if (needed != Items.AIR) {
                        findAndMoveItem(player, menu, needed, i, usedSlots);
                    }
                }
            }

            if (!menu.getSlot(4).hasItem()) {
                findAndMoveItem(player, menu, recipe.container(), 4, usedSlots);
            }
        });
        context.setPacketHandled(true);
    }

    private static void findAndMoveItem(ServerPlayer player, CauldronMenu menu, Item needed,
        int slotIndex, boolean[] usedSlots) {

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (usedSlots[i]) continue;

            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == needed) {
                ItemStack moved = player.getInventory().removeItem(i, 1);
                menu.getSlot(slotIndex).set(moved);

                if (stack.isEmpty()) usedSlots[i] = true;
                return;
            }
        }
    }

    private static void findAndMoveTagItem(ServerPlayer player, CauldronMenu menu,
        TagKey<Item> tag, int slotIndex, boolean[] usedSlots, CookingRecipe recipe) {

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (usedSlots[i]) continue;

            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(tag) && !recipe.isForbiddenCookedIngredient(stack.getItem())) {
                ItemStack moved = player.getInventory().removeItem(i, 1);
                menu.getSlot(slotIndex).set(moved);

                if (stack.isEmpty()) usedSlots[i] = true;
                return;
            }
        }
    }
}


