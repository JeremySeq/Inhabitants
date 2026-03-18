package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.ModSoundEvents;
import com.jeremyseq.inhabitants.entities.ModEntities;
import com.jeremyseq.inhabitants.items.food.*;

import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.*;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Inhabitants.MODID);

    public static final RegistryObject<Item> GIANT_BONE = ITEMS.register("giant_bone",
            GiantBoneItem::new);

    public static final RegistryObject<Item> FISH_SNOT_CHOWDER = ITEMS.register("fish_snot_chowder",
            FishSnotChowderItem::new);
    public static final RegistryObject<Item> UNCANNY_POTTAGE = ITEMS.register("uncanny_pottage",
            UncannyPottageItem::new);
    public static final RegistryObject<Item> SPIDER_SOUP = ITEMS.register("spider_soup",
            SpiderSoupItem::new);
    public static final RegistryObject<Item> BANEFUL_POTATO = ITEMS.register("baneful_potato",
            BanefulPotatoItem::new);
    public static final RegistryObject<Item> MONSTER_MEAL = ITEMS.register("monster_meal",
            MonsterMealItem::new);
    public static final RegistryObject<Item> DIMENSIONAL_SNACK = ITEMS.register("dimensional_snack",
            DimensionalSnackItem::new);

    public static final RegistryObject<Item> CREATIVE_TAB = ITEMS.register("creative_tab",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BOGRE_SPAWN_EGG = ITEMS.register("bogre_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.BOGRE, 0x36786A, 0xA35242, new Item.Properties()));

    public static final RegistryObject<Item> WARPED_CLAM_ITEM = ITEMS.register("warped_clam",
            () -> new WarpedClamItem(new Item.Properties()));

    public static final RegistryObject<Item> IMPALER_SPAWN_EGG = ITEMS.register("impaler_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.IMPALER, 0x969090, 0x9d9382, new Item.Properties()));

    public static final RegistryObject<Item> IMPALER_SPIKE = ITEMS.register("impaler_spike",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MUSIC_DISC_BOGRE = ITEMS.register("music_disc_bogre",
            () -> new RecordItem(6, ModSoundEvents.BOGRE_SONG, new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 2100));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
