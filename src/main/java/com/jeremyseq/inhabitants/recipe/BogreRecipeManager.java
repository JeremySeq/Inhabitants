package com.jeremyseq.inhabitants.recipe;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.items.ModItems;
import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import com.jeremyseq.inhabitants.networking.ModNetworking;
import com.jeremyseq.inhabitants.networking.bogre.BogreRecipePacketS2C;

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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.server.ServerLifecycleHooks;

import org.jetbrains.annotations.NotNull;

import com.google.gson.*;

import java.util.*;

public class BogreRecipeManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    private static List<CookingRecipe> cookingRecipes = new ArrayList<>();
    private static Map<Item, TransformationRecipe> transformationRecipes = new HashMap<>();
    private static Map<Block, CarvingRecipe> carvingRecipes = new HashMap<>();

    public BogreRecipeManager() {
        super(GSON, "bogre_recipes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject,
        @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
        
        List<CookingRecipe> cookingList = new ArrayList<>();
        Map<Item, TransformationRecipe> transformationMap = new HashMap<>();
        Map<Block, CarvingRecipe> carvingMap = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                parseRecipe(json, cookingList, transformationMap, carvingMap);
            } catch (Exception e) {
                Inhabitants.LOGGER.error("Failed to parse Bogre recipe: " + entry.getKey());
            }
        }

        addFallbackRecipes(cookingList, transformationMap, carvingMap);

        cookingRecipes = List.copyOf(cookingList);
        transformationRecipes = Map.copyOf(transformationMap);
        carvingRecipes = Map.copyOf(carvingMap);
        
        Inhabitants.LOGGER.info("Loaded Bogre recipes :D");
        
        broadcastRecipesToAllPlayers();
    }
    
    public static void broadcastRecipesToAllPlayers() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            BogreRecipePacketS2C packet = new BogreRecipePacketS2C(cookingRecipes);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ModNetworking.sendToPlayer(packet, player);
            }
        }
    }
    
    public static void sendBogreRecipesToPlayer(ServerPlayer player) {
        ModNetworking.sendToPlayer(new BogreRecipePacketS2C(cookingRecipes), player);
    }

    // --- Recipe Parsing ---

    private void parseRecipe(JsonObject json,
        List<CookingRecipe> cookingList, 
        Map<Item, TransformationRecipe> transformationMap, 
        Map<Block, CarvingRecipe> carvingMap) {
        try {
            String typeStr = GsonHelper.getAsString(json, "type", "cooking").toLowerCase();

            Item resultItem = ForgeRegistries.ITEMS.getValue(
                ResourceLocation.tryParse(GsonHelper.getAsString(json, "result")));

            int count = GsonHelper.getAsInt(json, "count", 1);
            if (resultItem == null || resultItem == Items.AIR) return;
            ItemStack result = new ItemStack(resultItem, count);

            switch (typeStr) {
                case "cooking" -> {
                    List<Item> ingredients = new ArrayList<>();
                    List<TagKey<Item>> tagIngredients = new ArrayList<>();
                    if (json.has("ingredients")) {
                        
                        JsonArray ingArray = json.getAsJsonArray("ingredients");
                        for (JsonElement ingEl : ingArray) {
                            String value = ingEl.getAsString();
                            if (value.startsWith("#")) {
                                ResourceLocation tagRL = ResourceLocation.tryParse(value.substring(1));
                                TagKey<Item> tag = TagKey.create(Registries.ITEM, tagRL);
                                tagIngredients.add(tag);
                        
                                Item fallback = tagRL.getPath().contains("fish") ? Items.COD : Items.AIR;
                                ingredients.add(fallback);
                            } else {
                                Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(value));
                                if (item != null && item != Items.AIR) {
                                    ingredients.add(item);
                                    tagIngredients.add(null);
                                }
                            }
                        }
                    }
                    
                    Item container = Items.BOWL;
                    
                    if (json.has("container")) {
                        container = ForgeRegistries.ITEMS.getValue(
                            ResourceLocation.tryParse(GsonHelper.getAsString(json, "container")));
                    }
                    if (container == null) container = Items.BOWL;

                    int time_ticks = GsonHelper.getAsInt(json, "time_ticks", GsonHelper.getAsInt(json, "timeTicks", 160));
                    cookingList.add(new CookingRecipe(ingredients, tagIngredients, container, result, time_ticks));
                }
                case "transformation" -> {
                    Item ingredient = ForgeRegistries.ITEMS.getValue(
                        ResourceLocation.tryParse(GsonHelper.getAsString(json, "ingredient")));
                    if (ingredient == null) return;
                    int hammer_hits = GsonHelper.getAsInt(json, "hammer_hits", 1);
                    Optional<SoundEvent> hammerSound = Optional.empty();
                    if (json.has("hammer_sound")) {
                        hammerSound = Optional.ofNullable(ForgeRegistries.SOUND_EVENTS.getValue(
                            ResourceLocation.tryParse(GsonHelper.getAsString(json, "hammer_sound"))));
                    }
                    transformationMap.put(ingredient, new TransformationRecipe(ingredient, result, hammer_hits, hammerSound));
                }
                case "carving" -> {
                    Block triggerBlock = ForgeRegistries.BLOCKS.getValue(
                        ResourceLocation.tryParse(GsonHelper.getAsString(json, "block")));
                    if (triggerBlock == null) return;
                    int requiredBlocks = GsonHelper.getAsInt(json, "required_blocks", 1);
                    int hammer_hits = GsonHelper.getAsInt(json, "hammer_hits", 1);
                    Optional<SoundEvent> hammerSound = Optional.empty();
                    if (json.has("hammer_sound")) {
                        hammerSound = Optional.ofNullable(ForgeRegistries.SOUND_EVENTS.getValue(
                            ResourceLocation.tryParse(GsonHelper.getAsString(json, "hammer_sound"))));
                    }
                    carvingMap.put(triggerBlock, new CarvingRecipe(triggerBlock, requiredBlocks, result, hammer_hits, hammerSound));
                }
            }
        } catch (Exception ignored) {}
    }

    // --- Fallback Recipes ---

    private void addFallbackRecipes(List<CookingRecipe> cookingList, Map<Item,
            TransformationRecipe> mutableTransformation, Map<Block, CarvingRecipe> mutableCarving) {
        
        if (!mutableCarving.containsKey(Blocks.BONE_BLOCK)) {
            mutableCarving.put(Blocks.BONE_BLOCK, new CarvingRecipe(
                Blocks.BONE_BLOCK,
                3,
                new ItemStack(ModItems.GIANT_BONE.get()),
                7,
                Optional.of(SoundEvents.BONE_BLOCK_HIT)));
        }

        if (!mutableTransformation.containsKey(Items.MUSIC_DISC_11)) {
            mutableTransformation.put(Items.MUSIC_DISC_11, new TransformationRecipe(
                Items.MUSIC_DISC_11,
                new ItemStack(ModItems.MUSIC_DISC_BOGRE.get(), 1),
                7,
                Optional.of(ModSoundEvents.BOGRE_HAMMER_KNOCK.get())));
        }
        
        if (cookingList.isEmpty()) {
            addFishSnotChowderFallback(cookingList);

            addCookingFallback(cookingList, 
                List.of(Items.SPIDER_EYE, Items.FERMENTED_SPIDER_EYE, Items.SLIME_BALL),
                ModItems.MARINATED_SPIDER, 30);
                
            addCookingFallback(cookingList,
                List.of(Items.POISONOUS_POTATO, Items.FERMENTED_SPIDER_EYE, Items.BONE),
                ModItems.UNCANNY_POTTAGE, 100);
                
            addCookingFallback(cookingList,
                List.of(Items.GUNPOWDER, Items.ROTTEN_FLESH, Items.ROTTEN_FLESH),
                ModItems.BAKED_BRAINS, 100);
                
            addCookingFallback(cookingList,
                List.of(Items.PHANTOM_MEMBRANE, Items.PHANTOM_MEMBRANE,
                Items.ENDER_PEARL), ModItems.DIMENSIONAL_SERVING, 100);
        }
    }


    // --- Recipe Accessors ---

    public static List<CookingRecipe> getAllCookingRecipes() {
        return cookingRecipes;
    }
    
    public static void setClientCookingRecipes(List<CookingRecipe> recipes) {
        cookingRecipes = List.copyOf(recipes);
    }

    public static Map<Item, TransformationRecipe> getTransformationRecipes() {
        return transformationRecipes;
    }

    public static Map<Block, CarvingRecipe> getCarvingRecipes() {
        return carvingRecipes;
    }

    public static Optional<CookingRecipe> getCookingRecipe(List<Item> items) {
        if (items.isEmpty()) return Optional.empty();
        
        for (CookingRecipe recipe : cookingRecipes) {
            if (recipe.ingredients().size() != items.size()) continue;

            if (isFishSnotChowderRecipe(recipe) && containsCookedItem(items)) continue;

            if (recipe.hasAnyTags()) {
                if (matchesTagRecipe(recipe, items)) return Optional.of(recipe);
            } else {
                if (matchesExactRecipe(recipe, items)) return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    private static boolean isFishSnotChowderRecipe(CookingRecipe recipe) {
        return recipe.result().is(ModItems.FISH_SNOT_CHOWDER.get());
    }

    private static boolean containsCookedItem(List<Item> items) {
        for (Item item : items) {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(item);
            if (rl != null && rl.getPath().contains("cooked")) return true;
        }
        return false;
    }

    private static boolean matchesExactRecipe(CookingRecipe recipe, List<Item> items) {
        List<Item> sortedItems = items.stream().filter(i -> i != Items.AIR)
            .sorted(Comparator.comparing(i -> ForgeRegistries.ITEMS.getKey(i).toString()))
            .toList();

        List<Item> sortedRecipeIngs = recipe.ingredients().stream()
            .sorted(Comparator.comparing(i -> ForgeRegistries.ITEMS.getKey(i).toString()))
            .toList();
        
        return sortedRecipeIngs.equals(sortedItems);
    }

    private static boolean matchesTagRecipe(CookingRecipe recipe, List<Item> items) {
        boolean[] usedItems = new boolean[items.size()];

        for (int slot = 0; slot < recipe.ingredients().size(); slot++) {
            TagKey<Item> tag = recipe.getTagForSlot(slot);
            boolean found = false;

            for (int j = 0; j < items.size(); j++) {
                if (usedItems[j]) continue;
                
                boolean matches;
                if (tag != null) {
                    matches = new ItemStack(items.get(j)).is(tag);
                } else {
                    matches = items.get(j) == recipe.ingredients().get(slot);
                }

                if (matches) {
                    usedItems[j] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    public static Optional<TransformationRecipe> getTransformationRecipe(Item item) {
        return Optional.ofNullable(transformationRecipes.get(item));
    }

    public static Optional<CarvingRecipe> getCarvingRecipe(Block block) {
        return Optional.ofNullable(carvingRecipes.get(block));
    }

    public static boolean isCookingIngredient(Item item) {
        if (item == Items.AIR) return false;
        ItemStack stack = new ItemStack(item);
        for (CookingRecipe recipe : cookingRecipes) {
            if (recipe.ingredients().contains(item)) return true;

            if (recipe.tagIngredients() != null) {
                for (TagKey<Item> tag : recipe.tagIngredients()) {
                    if (tag != null && stack.is(tag)) return true;
                }
            }
        }
        return false;
    }

    public static boolean isContainer(Item item) {
        for (CookingRecipe recipe : cookingRecipes) {
            if (recipe.container() == item) return true;
        }
        return item == Items.BOWL; // always allow bowl as fallback
    }

    public static boolean isTransformationIngredient(Item item) {
        return transformationRecipes.containsKey(item);
    }

    public static boolean isCarvable(Block block) {
        return carvingRecipes.containsKey(block);
    }

    // --- Helper Methods ---

    private static void addCookingFallback(List<CookingRecipe> list, List<Item> ingredients,
        RegistryObject<Item> resultItem, int time) {
            
        List<TagKey<Item>> tagList = new ArrayList<>(Collections.nCopies(ingredients.size(), null));
        list.add(new CookingRecipe(ingredients, tagList, Items.BOWL, new ItemStack(resultItem.get(), 1), time));
    }

    // --- Cooking Fallback Recipes ---
    private static void addFishSnotChowderFallback(List<CookingRecipe> list) {
        TagKey<Item> fishTag = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "cauldron_fish"));
        
        list.add(new CookingRecipe(
            List.of(Items.COD, Items.SALMON, Items.TROPICAL_FISH),
            List.of(fishTag, fishTag, fishTag),
            Items.BOWL,
            new ItemStack(ModItems.FISH_SNOT_CHOWDER.get(), 1),
            100));
    }
}