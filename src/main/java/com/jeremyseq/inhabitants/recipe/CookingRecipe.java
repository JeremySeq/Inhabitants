package com.jeremyseq.inhabitants.recipe;

import com.jeremyseq.inhabitants.items.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.TagKey;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public record CookingRecipe(
    List<Item> ingredients,
    List<TagKey<Item>> tagIngredients,
    Item container, // bowl, skeleton skull, etc.
    ItemStack result,
    int time_ticks
) implements IBogreRecipe {
    
    @Override
    public Type getBogreRecipeType() {
        return Type.COOKING;
    }

    public boolean hasTagIngredient(int slot) {
        return tagIngredients != null && slot < tagIngredients.size() &&
        tagIngredients.get(slot) != null;
    }

    public TagKey<Item> getTagForSlot(int slot) {
        return tagIngredients != null &&
        slot < tagIngredients.size() ? tagIngredients.get(slot) : null;
    }

    public boolean hasAnyTags() {
        if (tagIngredients == null) return false;
        return tagIngredients.stream().anyMatch(Objects::nonNull);
    }

    public Item getDisplayItem(int slot, long time) {
        if (hasTagIngredient(slot)) {
            TagKey<Item> tag = getTagForSlot(slot);
            if (tag != null) {
                try {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level != null) {
                        var reg = mc.level.registryAccess()
                                .registry(Registries.ITEM);

                        if (reg.isPresent()) {
                            var tagSet = reg.get().getTag(tag);
                            if (tagSet.isPresent() && tagSet.get().size() > 0) {
                                List<Item> items = tagSet.get().stream().map(Holder::value)
                                    .filter(item -> !isForbiddenCookedIngredient(item))
                                    .toList();
                                
                                if (!items.isEmpty()) {
                                    return items.get((int) ((time / 20) % items.size()));
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                try {
                    var tagValue = Objects.requireNonNull(ForgeRegistries.ITEMS.tags())
                            .getTag(tag);

                    if (!tagValue.isEmpty()) {
                        List<Item> tagItems = new ArrayList<>();
                        tagValue.forEach(item -> {
                            if (!isForbiddenCookedIngredient(item)) {
                                tagItems.add(item);
                            }
                        });
                        
                        if (!tagItems.isEmpty()) {
                            return tagItems.get((int) ((time / 20) % tagItems.size()));
                        }
                    }
                } catch (Exception ignored) {}

                String path = tag.location().getPath().toLowerCase();
                if (path.contains("fish") || path.contains("fishes")) {
                    List<Item> vanillaFishes = List.of(
                            Items.COD,
                            Items.SALMON,
                            Items.TROPICAL_FISH,
                            Items.PUFFERFISH);

                    return vanillaFishes.get((int) ((time / 20) % vanillaFishes.size()));
                }
            }
        }

        return slot < ingredients.size() ? ingredients.get(slot) : Items.AIR;
    }
    
    public boolean isForbiddenCookedIngredient(Item item) {
        if (!this.result.is(ModItems.FISH_SNOT_CHOWDER.get())) return false;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
        return rl != null && rl.getPath().contains("cooked");
    }
}
