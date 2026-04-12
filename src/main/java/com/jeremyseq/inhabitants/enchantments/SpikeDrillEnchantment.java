package com.jeremyseq.inhabitants.enchantments;

import com.jeremyseq.inhabitants.items.SpikeDrillItem;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;

public class SpikeDrillEnchantment extends Enchantment {
    private final int maxLevel;

    protected SpikeDrillEnchantment(
        Rarity rarity,
        EnchantmentCategory category,
        EquipmentSlot[] applicableSlots,
        int maxLevel
    ) {
        super(rarity, category, applicableSlots);
        this.maxLevel = maxLevel;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof SpikeDrillItem;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }
}
