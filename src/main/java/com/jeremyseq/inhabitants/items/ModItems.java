package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.entities.ModEntities;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
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
            () -> new ForgeSpawnEggItem(ModEntities.IMPALER, 0xA0925B, 0xC8C4BF, new Item.Properties()));

    public static final RegistryObject<Item> IMPALER_SPIKE = ITEMS.register("impaler_spike",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ANCIENT_FLUTE = ITEMS.register("ancient_flute",
            () -> new AncientFluteItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> WISHFISH_SPAWN_EGG = ITEMS.register("wishfish_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.WISHFISH, 0xE9B115, 0xFDF55F, new Item.Properties()));

    public static final RegistryObject<Item> WISHFISH_BUCKET = ITEMS.register("wishfish_bucket", () ->
            new MobBucketItem(
                    ModEntities.WISHFISH,
                    () -> Fluids.WATER,
                    () -> SoundEvents.BUCKET_EMPTY_FISH,
                    new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET))
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
