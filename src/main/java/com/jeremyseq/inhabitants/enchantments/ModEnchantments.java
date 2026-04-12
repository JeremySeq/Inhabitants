package com.jeremyseq.inhabitants.enchantments;

import com.jeremyseq.inhabitants.Inhabitants;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.*;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, Inhabitants.MODID);

    public static final RegistryObject<Enchantment> DIAMOND_TIP =
        ENCHANTMENTS.register("diamond_tip",
            () -> new SpikeDrillEnchantment(
                Enchantment.Rarity.RARE,
                EnchantmentCategory.DIGGER,
                new EquipmentSlot[]{EquipmentSlot.MAINHAND},
                1
            ));

    public static final RegistryObject<Enchantment> THERMAL_CAPACITY =
        ENCHANTMENTS.register("thermal_capacity",
            () -> new SpikeDrillEnchantment(
                Enchantment.Rarity.UNCOMMON,
                EnchantmentCategory.DIGGER,
                new EquipmentSlot[]{EquipmentSlot.MAINHAND},
                1
                ));

    public static void register(IEventBus eventBus) {
        ENCHANTMENTS.register(eventBus);
    }
}
