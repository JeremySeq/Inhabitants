package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.world.item.Item;
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

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
