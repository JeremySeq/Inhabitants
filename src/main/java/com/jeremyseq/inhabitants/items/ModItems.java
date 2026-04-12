package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import com.jeremyseq.inhabitants.entities.ModEntities;
import com.jeremyseq.inhabitants.items.food.*;
import com.jeremyseq.inhabitants.items.javelin.JavelinItem;
import com.jeremyseq.inhabitants.blocks.ModBlocks;

import net.minecraft.world.item.*;
import net.minecraft.core.Direction;

import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Inhabitants.MODID);

    public static final RegistryObject<Item> GIANT_BONE = ITEMS.register("giant_bone",
            GiantBoneItem::new);

    public static final RegistryObject<Item> FISH_SNOT_CHOWDER = ITEMS.register("fish_snot_chowder",
            FishSnotChowderItem::new);
    public static final RegistryObject<Item> UNCANNY_POTTAGE = ITEMS.register("uncanny_pottage",
            UncannyPottageItem::new);
    public static final RegistryObject<Item> MARINATED_SPIDER = ITEMS.register("marinated_spider",
            MarinatedSpiderItem::new);
    public static final RegistryObject<Item> BAKED_BRAINS = ITEMS.register("baked_brains",
            BakedBrainsItem::new);
    public static final RegistryObject<Item> DIMENSIONAL_SERVING = ITEMS.register("dimensional_serving",
            DimensionalServingItem::new);

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

    public static final RegistryObject<Item> JAVELIN = ITEMS.register("javelin",
            () -> new JavelinItem(new Item.Properties().stacksTo(16)));

    /* this item show's ONLY in JEI, so don't dream getting it in-game *kiss*, *kiss* */
    public static final RegistryObject<Item> BOGRE_CAULDRON = ITEMS.register("bogre_cauldron",
            () -> new Item(new Item.Properties()));
    /* the end of the joke, i hope */

    public static final RegistryObject<Item> SPIKE_DRILL = ITEMS.register("spike_drill",
            () -> new SpikeDrillItem(new Item.Properties().defaultDurability(2342)));

    public static final RegistryObject<Item> IMPALER_HEAD = ITEMS.register("impaler_head",
            () -> new StandingAndWallBlockItem(ModBlocks.IMPALER_HEAD.get(),
            ModBlocks.IMPALER_WALL_HEAD.get(),
            new Item.Properties().rarity(Rarity.UNCOMMON),
            Direction.DOWN));

    public static final RegistryObject<Item> DRIPSTONE_IMPALER_HEAD = ITEMS.register("dripstone_impaler_head",
            () -> new StandingAndWallBlockItem(ModBlocks.DRIPSTONE_IMPALER_HEAD.get(),
            ModBlocks.DRIPSTONE_IMPALER_WALL_HEAD.get(),
            new Item.Properties().rarity(Rarity.UNCOMMON),
            Direction.DOWN));

    public static final RegistryObject<Item> CONCHER_SPAWN_EGG = ITEMS.register("concher_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CONCHER, 0x91C8C7, 0x527499, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
