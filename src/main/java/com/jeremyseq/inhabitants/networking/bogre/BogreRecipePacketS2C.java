package com.jeremyseq.inhabitants.networking.bogre;

import com.jeremyseq.inhabitants.recipe.CookingRecipe;
import com.jeremyseq.inhabitants.recipe.BogreRecipeManager;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.function.Supplier;

public class BogreRecipePacketS2C {

    private final List<CookingRecipe> recipes;

    public BogreRecipePacketS2C(List<CookingRecipe> recipes) {
        this.recipes = recipes;
    }
    
    public static void encode(BogreRecipePacketS2C packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.recipes.size());
        for (CookingRecipe recipe : packet.recipes) {
            buf.writeInt(recipe.ingredients().size());
            for (int i = 0; i < recipe.ingredients().size(); i++) {
                Item item = recipe.ingredients().get(i);
                buf.writeUtf(ForgeRegistries.ITEMS.getKey(item).toString());
                
                boolean hasTag = recipe.hasTagIngredient(i);
                buf.writeBoolean(hasTag);
                if (hasTag) {
                    buf.writeUtf(recipe.getTagForSlot(i).location().toString());
                }
            }
            
            buf.writeUtf(ForgeRegistries.ITEMS.getKey(recipe.container()).toString());
            buf.writeItem(recipe.result());
            buf.writeInt(recipe.time_ticks());
        }
    }
    
    public static BogreRecipePacketS2C decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<CookingRecipe> recipes = new ArrayList<>();

        for (int r = 0; r < count; r++) {
            int ingCount = buf.readInt();
            List<Item> ingredients = new ArrayList<>();
            List<TagKey<Item>> tagIngredients = new ArrayList<>();
            for (int i = 0; i < ingCount; i++) {
                Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(buf.readUtf()));
                ingredients.add(item != null ? item : Items.AIR);

                boolean hasTag = buf.readBoolean();
                if (hasTag) {
                    tagIngredients.add(TagKey.create(Registries.ITEM, ResourceLocation.tryParse(buf.readUtf())));
                } else {
                    tagIngredients.add(null);
                }
            }
            
            Item container = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(buf.readUtf()));
            ItemStack result = buf.readItem();
            int timeTicks = buf.readInt();

            recipes.add(new CookingRecipe(
                ingredients, 
                tagIngredients, 
                container != null ? container : Items.BOWL, 
                result, 
                timeTicks));
        }

        return new BogreRecipePacketS2C(recipes);
    }
    
    public static void handle(BogreRecipePacketS2C packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        
        context.enqueueWork(() -> {
            BogreRecipeManager.setClientCookingRecipes(packet.recipes);
        });

        context.setPacketHandled(true);
    }
}