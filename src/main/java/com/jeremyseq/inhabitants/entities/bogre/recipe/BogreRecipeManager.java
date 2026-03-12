package com.jeremyseq.inhabitants.entities.bogre.recipe;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.items.ModItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

public class BogreRecipeManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    private static Map<Item, BogreRecipe> cookingRecipes = ImmutableMap.of();
    private static Map<Item, BogreRecipe> transformationRecipes = ImmutableMap.of();
    private static Map<Block, BogreRecipe> carvingRecipes = ImmutableMap.of();

    public BogreRecipeManager() {
        super(GSON, "bogre_recipes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
        ImmutableMap.Builder<Item, BogreRecipe> cookingBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<Item, BogreRecipe> transformationBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<Block, BogreRecipe> carvingBuilder = ImmutableMap.builder();

        for (Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                String typeStr = GsonHelper.getAsString(json, "type", "cooking").toUpperCase();
                BogreRecipe.Type type = BogreRecipe.Type.valueOf(typeStr);

                Item resultItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(GsonHelper.getAsString(json, "result")));
                int count = GsonHelper.getAsInt(json, "count", 1);
                ItemStack result = new ItemStack(resultItem, count);
                int timeTicks = GsonHelper.getAsInt(json, "time_ticks", 160);
                float stewChance = GsonHelper.getAsFloat(json, "suspicious_stew_chance", 0.3f);

                Optional<Item> triggerItem = Optional.empty();
                Optional<Block> triggerBlock = Optional.empty();

                if (json.has("ingredient")) {
                    triggerItem = Optional.ofNullable(ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(GsonHelper.getAsString(json, "ingredient"))));
                }
                if (json.has("block")) {
                    triggerBlock = Optional.ofNullable(ForgeRegistries.BLOCKS.getValue(ResourceLocation.tryParse(GsonHelper.getAsString(json, "block"))));
                }
                int requiredBlocks = GsonHelper.getAsInt(json, "required_blocks", 1);

                Optional<SoundEvent> hammerSound = Optional.empty();
                if (json.has("hammer_sound")) {
                    hammerSound = Optional.ofNullable(ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.tryParse(GsonHelper.getAsString(json, "hammer_sound"))));
                }

                BogreRecipe recipe = new BogreRecipe(type, triggerItem, triggerBlock, result, stewChance, timeTicks, requiredBlocks, hammerSound);

                switch (type) {
                    case COOKING -> triggerItem.ifPresent(item -> cookingBuilder.put(item, recipe));
                    case TRANSFORMATION -> triggerItem.ifPresent(item -> transformationBuilder.put(item, recipe));
                    case CARVING -> triggerBlock.ifPresent(block -> carvingBuilder.put(block, recipe));
                }
            } catch (Exception e) {
                Inhabitants.LOGGER.error("Failed to parse Bogre recipe");
            }
        }

        Map<Item, BogreRecipe> mutableCooking = new HashMap<>(cookingBuilder.build());
        Map<Item, BogreRecipe> mutableTransformation = new HashMap<>(transformationBuilder.build());
        Map<Block, BogreRecipe> mutableCarving = new HashMap<>(carvingBuilder.build());

        // if recipe is missing, add a fallback recipe
        if (!mutableCarving.containsKey(Blocks.BONE_BLOCK)) {
            mutableCarving.put(Blocks.BONE_BLOCK, new BogreRecipe(
                    BogreRecipe.Type.CARVING, Optional.empty(), Optional.of(Blocks.BONE_BLOCK), 
                    new ItemStack(ModItems.GIANT_BONE.get(), 1), 0.0f, 130, 3, Optional.empty()));
        }

        if (!mutableTransformation.containsKey(Items.MUSIC_DISC_11)) {
            mutableTransformation.put(Items.MUSIC_DISC_11, new BogreRecipe(
                    BogreRecipe.Type.TRANSFORMATION, Optional.of(Items.MUSIC_DISC_11), Optional.empty(), 
                    new ItemStack(ModItems.MUSIC_DISC_BOGRE.get(), 1), 0.0f, 100, 1, Optional.empty()));
        }

        if (mutableCooking.isEmpty()) {
            record Fallback(Item in, RegistryObject<Item> out, float chance, int time) {}
            var defaults = new Fallback[] {
                new Fallback(Items.COD, ModItems.FISH_SNOT_CHOWDER, 0.3f, 160),
                new Fallback(Items.COOKED_COD, ModItems.FISH_SNOT_CHOWDER, 0.3f, 160),
                new Fallback(Items.SALMON, ModItems.FISH_SNOT_CHOWDER, 0.3f, 160),
                new Fallback(Items.COOKED_SALMON, ModItems.FISH_SNOT_CHOWDER, 0.3f, 160),
                new Fallback(Items.PUFFERFISH, ModItems.UNCANNY_POTTAGE, 0.5f, 160),
                new Fallback(Items.TROPICAL_FISH, ModItems.FISH_SNOT_CHOWDER, 0.3f, 160),
                new Fallback(Items.ROTTEN_FLESH, ModItems.STINKY_BOUILLON, 0.4f, 200),
                new Fallback(Items.SPIDER_EYE, ModItems.UNCANNY_POTTAGE, 0.6f, 200)
            };
            
            for (Fallback f : defaults) {
                addCookingFallback(mutableCooking, f.in(), f.out(), f.chance(), f.time());
            }
        }

        cookingRecipes = ImmutableMap.copyOf(mutableCooking);
        transformationRecipes = ImmutableMap.copyOf(mutableTransformation);
        carvingRecipes = ImmutableMap.copyOf(mutableCarving);
        
        Inhabitants.LOGGER.info("Loaded Bogre recipes");
    }

    public static Optional<BogreRecipe> getCookingRecipe(Item item) {
        return Optional.ofNullable(cookingRecipes.get(item));
    }

    public static Optional<BogreRecipe> getTransformationRecipe(Item item) {
        return Optional.ofNullable(transformationRecipes.get(item));
    }

    public static Optional<BogreRecipe> getCarvingRecipe(Block block) {
        return Optional.ofNullable(carvingRecipes.get(block));
    }

    public static boolean isCookingIngredient(Item item) {
        return cookingRecipes.containsKey(item);
    }

    public static boolean isTransformationIngredient(Item item) {
        return transformationRecipes.containsKey(item);
    }

    public static boolean isCarvable(Block block) {
        return carvingRecipes.containsKey(block);
    }

    private static void addCookingFallback(Map<Item, BogreRecipe> map, Item ingredient, RegistryObject<Item> resultItem, float stewChance, int timeTicks) {
        map.put(ingredient, new BogreRecipe(BogreRecipe.Type.COOKING, Optional.of(ingredient), Optional.empty(),
                new ItemStack(resultItem.get(), 1), stewChance, timeTicks, 1, Optional.empty()));
    }
}
