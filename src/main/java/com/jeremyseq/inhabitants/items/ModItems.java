package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.blocks.ModBlocks;
import com.jeremyseq.inhabitants.entities.ModEntities;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Inhabitants.MODID);

    public static final RegistryObject<Item> GIANT_BONE = ITEMS.register("giant_bone",
            GiantBoneItem::new);

    public static final RegistryObject<Item> FISH_SNOT_CHOWDER = ITEMS.register("fish_snot_chowder",
            FishSnotChowderItem::new);

    public static final RegistryObject<Item> BRACER_OF_MIGHT = ITEMS.register("bracer_of_might",
            BracerOfMightItem::new);

    public static final RegistryObject<Item> DEAD_BOGRE_ICON = ITEMS.register("dead_bogre_icon",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BOGRE_SPAWN_EGG = ITEMS.register("bogre_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.BOGRE, 0x36786A, 0xA35242, new Item.Properties()));

    public static final RegistryObject<Item> WARPED_CLAM_ITEM = ITEMS.register("warped_clam",
            () -> new WarpedClamItem(new Item.Properties()));

    public static final RegistryObject<Item> RAW_ABYSSFISH = ITEMS.register("raw_abyssfish",
            RawAbyssfishItem::new);

    public static final RegistryObject<Item> COOKED_ABYSSFISH = ITEMS.register("cooked_abyssfish",
            CookedAbyssfishItem::new);

    public static final RegistryObject<Item> ABYSSFISH_BUCKET = ITEMS.register("abyssfish_bucket", () ->
            new MobBucketItem(
                    ModEntities.ABYSSFISH,
                    () -> Fluids.WATER,
                    () -> SoundEvents.BUCKET_EMPTY_FISH,
                    new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET))
    );

    public static final RegistryObject<Item> ABYSSFISH_SPAWN_EGG = ITEMS.register("abyssfish_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ABYSSFISH, 0x509297, 0xA9CDD1, new Item.Properties()));

    public static final RegistryObject<Item> IMPALER_SPAWN_EGG = ITEMS.register("impaler_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.IMPALER, 0xAB7C53, 0x8E826F, new Item.Properties()));

    public static final RegistryObject<Item> IMPALER_SPIKE = ITEMS.register("impaler_spike",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ANCIENT_FLUTE = ITEMS.register("ancient_flute",
            () -> new AncientFluteItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> WISHFISH = ITEMS.register("wishfish",
            () -> new Item((new Item.Properties()).rarity(Rarity.RARE).food(Foods.GOLDEN_APPLE)));

    public static final RegistryObject<Item> WISHFISH_SPAWN_EGG = ITEMS.register("wishfish_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.WISHFISH, 0xE9B115, 0xFDF55F, new Item.Properties()));

    public static final RegistryObject<Item> WISHFISH_BUCKET = ITEMS.register("wishfish_bucket", () ->
            new MobBucketItem(
                    ModEntities.WISHFISH,
                    () -> Fluids.WATER,
                    () -> SoundEvents.BUCKET_EMPTY_FISH,
                    new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET).rarity(Rarity.EPIC))
    );

    public static final RegistryObject<Item> GAZER_POD = ITEMS.register("gazer_pod_item",
            () -> new GazerPodItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> APEX_SPAWN_EGG = ITEMS.register("apex_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.APEX, 0x6C7D8D, 0x79F0F4, new Item.Properties()));

    public static final RegistryObject<Item> APEX_HORN = ITEMS.register("apex_horn",
            () -> new ApexHorn(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> GAZER_SPAWN_EGG = ITEMS.register("gazer_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GAZER, 0x8F0B48, 0xE1778D, new Item.Properties()));

    public static final RegistryObject<Item> WATERBERRY_BUSH_ITEM =
            ITEMS.register("waterberry_bush", () -> new BlockItem(
                    ModBlocks.WATERBERRY_BLOCK.get(),
                    new Item.Properties()
            ));

    public static final RegistryObject<Item> WATERBERRY =
            ITEMS.register("waterberry", () -> new WaterberryItem(
                    new Item.Properties().stacksTo(16)
            ));

    public static final RegistryObject<Item> CATCHER_SPAWN_EGG = ITEMS.register("catcher_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CATCHER, 0x664E80, 0xA46D3C, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
