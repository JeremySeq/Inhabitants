package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.entities.ModEntities;
import net.minecraft.world.item.*;
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
    public static final RegistryObject<Item> UNCANNY_POTTAGE = ITEMS.register("uncanny_pottage",
            UncannyPottageItem::new);
    public static final RegistryObject<Item> STINKY_BOUILLON = ITEMS.register("stinky_bouillon",
            StinkyBouillonItem::new);

    public static final RegistryObject<Item> DEAD_BOGRE_ICON = ITEMS.register("dead_bogre_icon",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BOGRE_SPAWN_EGG = ITEMS.register("bogre_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.BOGRE, 0x36786A, 0xA35242, new Item.Properties()));

    public static final RegistryObject<Item> WARPED_CLAM_ITEM = ITEMS.register("warped_clam",
            () -> new WarpedClamItem(new Item.Properties()));

    public static final RegistryObject<Item> IMPALER_SPAWN_EGG = ITEMS.register("impaler_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.IMPALER, 0x969090, 0x9d9382, new Item.Properties()));

    public static final RegistryObject<Item> IMPALER_SPIKE = ITEMS.register("impaler_spike",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> APEX_SPAWN_EGG = ITEMS.register("apex_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.APEX, 0x6C7D8D, 0x79F0F4, new Item.Properties()));

    public static final RegistryObject<Item> APEX_HORN = ITEMS.register("apex_horn",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> CORNUCOPIA = ITEMS.register("cornucopia",
            () -> new Cornucopia(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> BOULDER_SPAWN_EGG = ITEMS.register("boulder_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.BOULDER, 0x636363, 0xBDBDBD, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
