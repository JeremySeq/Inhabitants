package com.jeremyseq.inhabitants.entities.bogre.recipe;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.items.ModItems;
import com.jeremyseq.inhabitants.entities.bogre.recipe.BogreRecipe.StewEffect;

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
import net.minecraft.world.effect.MobEffect;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableMap;
import com.google.gson.*;

import java.util.*;

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
                Optional<BogreRecipe> recipeOpt = parseRecipe(json);
                
                recipeOpt.ifPresent(recipe -> {
                    switch (recipe.type()) {
                        case COOKING -> recipe.triggerItem()
                        .ifPresent(item -> cookingBuilder.put(item, recipe));

                        case TRANSFORMATION -> recipe.triggerItem()
                        .ifPresent(item -> transformationBuilder.put(item, recipe));

                        case CARVING -> recipe.triggerBlock()
                        .ifPresent(block -> carvingBuilder.put(block, recipe));
                    }
                });
            } catch (Exception e) {
                Inhabitants.LOGGER.error("Failed to parse Bogre recipe");
            }
        }

        Map<Item, BogreRecipe> mutableCooking = new HashMap<>(cookingBuilder.build());
        Map<Item, BogreRecipe> mutableTransformation = new HashMap<>(transformationBuilder.build());
        Map<Block, BogreRecipe> mutableCarving = new HashMap<>(carvingBuilder.build());

        // if recipe is missing, add a fallback recipe
        addFallbackRecipes(mutableCooking, mutableTransformation, mutableCarving);

        cookingRecipes = ImmutableMap.copyOf(mutableCooking);
        transformationRecipes = ImmutableMap.copyOf(mutableTransformation);
        carvingRecipes = ImmutableMap.copyOf(mutableCarving);
        
        Inhabitants.LOGGER.info("Loaded Bogre recipes");
    }

    // --- Recipe Parsing ---
    private Optional<BogreRecipe> parseRecipe(JsonObject json) {
        try {
            String typeStr = GsonHelper.getAsString(json, "type", "cooking").toUpperCase();
            BogreRecipe.Type type = BogreRecipe.Type.valueOf(typeStr);

            Item resultItem = ForgeRegistries.ITEMS.getValue(
                ResourceLocation.tryParse(GsonHelper.getAsString(json, "result")));
                
            int count = GsonHelper.getAsInt(json, "count", 1);
            if (resultItem == null) return Optional.empty();
            
            ItemStack result = new ItemStack(resultItem, count);
            float stewChance = GsonHelper.getAsFloat(json, "suspicious_stew_chance", 0.3f);
            
            List<BogreRecipe.StewEffect> stewEffects = new ArrayList<>();
            if (json.has("stew_effects")) {
                JsonArray effectsArray = json.getAsJsonArray("stew_effects");
                for (JsonElement effectEl : effectsArray) {
                    JsonObject effectObj = effectEl.getAsJsonObject();

                    MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(
                        ResourceLocation.tryParse(GsonHelper.getAsString(effectObj, "effect")));

                    int duration = GsonHelper.getAsInt(effectObj, "duration", 200);

                    if (effect != null) {
                        stewEffects.add(new StewEffect(effect, duration));
                    }
                }
            }

            Optional<Item> triggerItem = Optional.empty();
            if (json.has("ingredient")) {
                triggerItem = Optional.ofNullable(
                    ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(
                        GsonHelper.getAsString(json, "ingredient"))));
            }

            Optional<Block> triggerBlock = Optional.empty();
            if (json.has("block")) {
                triggerBlock = Optional.ofNullable(ForgeRegistries.BLOCKS.getValue(
                    ResourceLocation.tryParse(GsonHelper.getAsString(json, "block"))));
            }

            int requiredBlocks = GsonHelper.getAsInt(json, "required_blocks", 1);
            int time_ticks = GsonHelper.getAsInt(json, "time_ticks", GsonHelper.getAsInt(json, "timeTicks", 160));
            int hammer_hits = GsonHelper.getAsInt(json, "hammer_hits", 1);

            Optional<SoundEvent> hammerSound = Optional.empty();
            if (json.has("hammer_sound")) {
                hammerSound = Optional.ofNullable(ForgeRegistries.SOUND_EVENTS.getValue(
                    ResourceLocation.tryParse(GsonHelper.getAsString(json, "hammer_sound"))));
            }

            return Optional.of(new BogreRecipe(type, triggerItem, triggerBlock, result,
            stewChance, stewEffects, requiredBlocks, hammerSound, time_ticks, hammer_hits));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // --- Fallback Recipes ---
    private void addFallbackRecipes(Map<Item, BogreRecipe> mutableCooking, Map<Item,
    BogreRecipe> mutableTransformation, Map<Block, BogreRecipe> mutableCarving) {
        
        if (!mutableCarving.containsKey(Blocks.BONE_BLOCK)) {
            mutableCarving.put(Blocks.BONE_BLOCK, new BogreRecipe(
                    BogreRecipe.Type.CARVING, Optional.empty(), Optional.of(Blocks.BONE_BLOCK), 
                    new ItemStack(ModItems.GIANT_BONE.get()), 0.0f, List.of(),
                    1, Optional.empty(), 160, 7));
        }

        if (!mutableTransformation.containsKey(Items.MUSIC_DISC_11)) {
            mutableTransformation.put(Items.MUSIC_DISC_11, new BogreRecipe(
                    BogreRecipe.Type.TRANSFORMATION, Optional.of(Items.MUSIC_DISC_11), Optional.empty(), 
                    new ItemStack(ModItems.MUSIC_DISC_BOGRE.get(), 1), 0.0f,
                    List.of(), 1, Optional.empty(), 160, 7));
        }

        if (mutableCooking.isEmpty()) {
            record Fallback(Item in, RegistryObject<Item> out, float chance) {}
            var defaults = new Fallback[] {
                new Fallback(Items.COD, ModItems.FISH_SNOT_CHOWDER, 0.3f),
                new Fallback(Items.COOKED_COD, ModItems.FISH_SNOT_CHOWDER, 0.3f),
                new Fallback(Items.SALMON, ModItems.FISH_SNOT_CHOWDER, 0.3f),
                new Fallback(Items.COOKED_SALMON, ModItems.FISH_SNOT_CHOWDER, 0.3f),
                new Fallback(Items.PUFFERFISH, ModItems.UNCANNY_POTTAGE, 0.5f),
                new Fallback(Items.TROPICAL_FISH, ModItems.FISH_SNOT_CHOWDER, 0.3f),
                new Fallback(Items.SPIDER_EYE, ModItems.SPIDER_SOUP, 0.2f),
                new Fallback(Items.POISONOUS_POTATO, ModItems.BANEFUL_POTATO, 0.3f),
                new Fallback(Items.ROTTEN_FLESH, ModItems.MONSTER_MEAL, 0.4f),
                new Fallback(Items.ENDER_PEARL, ModItems.DIMENSIONAL_SNACK, 0.5f)
            };
            
            for (Fallback f : defaults) {
                addCookingFallback(mutableCooking, f.in(), f.out(), f.chance());
            }
        }
    }


    // --- Recipe Accessors ---
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

    // --- Helper Methods ---
    private static ItemStack createStew(Item ingredient, float chance) {
        if (ingredient == Items.COD || ingredient == Items.COOKED_COD || 
            ingredient == Items.SALMON || ingredient == Items.COOKED_SALMON || 
            ingredient == Items.TROPICAL_FISH) {
            return new ItemStack(ModItems.FISH_SNOT_CHOWDER.get());
        }
        if (ingredient == Items.PUFFERFISH) {
            return new ItemStack(ModItems.UNCANNY_POTTAGE.get());
        }
        if (ingredient == Items.SPIDER_EYE) {
            return new ItemStack(ModItems.SPIDER_SOUP.get());
        }
        if (ingredient == Items.POISONOUS_POTATO) {
            return new ItemStack(ModItems.BANEFUL_POTATO.get());
        }
        if (ingredient == Items.ROTTEN_FLESH) {
            return new ItemStack(ModItems.MONSTER_MEAL.get());
        }
        if (ingredient == Items.ENDER_PEARL) {
            return new ItemStack(ModItems.DIMENSIONAL_SNACK.get());
        }
        return null;
    }

    private static void addCookingFallback(Map<Item, BogreRecipe> map, Item ingredient,
    RegistryObject<Item> resultItem, float stewChance) {
        
        ItemStack result = createStew(ingredient, stewChance);
        if (result == null) {
            result = new ItemStack(resultItem.get(), 1);
        }

        map.put(ingredient, new BogreRecipe(BogreRecipe.Type.COOKING,
        Optional.of(ingredient), Optional.empty(),
        result, stewChance, List.of(), 1, Optional.empty(), 160, 1));
    }
}
