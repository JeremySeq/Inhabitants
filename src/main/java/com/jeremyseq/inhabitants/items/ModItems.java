package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.entities.ModEntities;
import com.jeremyseq.inhabitants.items.armor.ModArmorMaterials;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

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

    public static final RegistryObject<Item> BRACER_OF_MIGHT = ITEMS.register("bracer_of_might",
            BracerOfMightItem::new);

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

    public static final RegistryObject<Item> ANCIENT_FLUTE = ITEMS.register("ancient_flute",
            () -> new AncientFluteItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GAZER_POD = ITEMS.register("gazer_pod_item",
            () -> new GazerPodItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> APEX_SPAWN_EGG = ITEMS.register("apex_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.APEX, 0x6C7D8D, 0x79F0F4, new Item.Properties()));

    public static final RegistryObject<Item> APEX_HORN = ITEMS.register("apex_horn",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> CORNUCOPIA = ITEMS.register("cornucopia",
            () -> new Cornucopia(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> GAZER_SPAWN_EGG = ITEMS.register("gazer_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GAZER, 0x8F0B48, 0xE1778D, new Item.Properties()));

    public static final RegistryObject<Item> WATERBERRY =
            ITEMS.register("waterberry", () -> new WaterberryItem(
                    new Item.Properties().stacksTo(16)
            ));

    public static final RegistryObject<Item> CATCHER_SPAWN_EGG = ITEMS.register("catcher_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CATCHER, 0x664E80, 0xA46D3C, new Item.Properties()));

    public static final RegistryObject<Item> CHITIN_CHESTPLATE = ITEMS.register("chitin_chestplate",
            () -> new ChitinChestplateItem(ModArmorMaterials.CHITIN, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> CHITIN_CHESTPLATE_ELYTRA = ITEMS.register("chitin_chestplate_elytra",
            ChitinChestplateElytraItem::new);

    public static final RegistryObject<Item> CHITIN_SHIELD = ITEMS.register("chitin_shield",
            () -> new ShieldItem(new Item.Properties().durability(750)));

    public static final RegistryObject<Item> CHITIN_UPGRADE_SMITHING_TEMPLATE = ITEMS.register(
            "chitin_upgrade_smithing_template",
            () -> new SmithingTemplateItem(
                    Component.translatable("upgrade.chitin.applies_to").withStyle(ChatFormatting.BLUE),
                    Component.translatable("upgrade.chitin.ingredients").withStyle(ChatFormatting.BLUE),
                    Component.translatable("upgrade.chitin.title").withStyle(ChatFormatting.GRAY),
                    Component.translatable("upgrade.chitin.base_slot_description"),
                    Component.translatable("upgrade.chitin.additions_slot_description"),
                    List.of(ResourceLocation.fromNamespaceAndPath("inhabitants", "item/empty_shield_slot")),
                    List.of(ResourceLocation.fromNamespaceAndPath("inhabitants", "item/empty_chitin_slot"))
            )
    );


    public static final RegistryObject<Item> CHITIN = ITEMS.register("chitin",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BOULDER_SPAWN_EGG = ITEMS.register("boulder_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.BOULDER, 0x636363, 0xBDBDBD, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
