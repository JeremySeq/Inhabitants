package com.jeremyseq.inhabitants.entities.bogre;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

import java.util.Collection;

/**
 * this is a utility class for BogreEntity
 */

public class BogreUtil {
    
    private static final TagKey<Item> TAG_SWORDS = forge("tools/swords");
    private static final TagKey<Item> TAG_TRIDENTS = forge("tools/tridents");
    private static final TagKey<Item> TAG_BOWS = forge("tools/bows");
    private static final TagKey<Item> TAG_CROSSBOWS = forge("tools/crossbows");
    private static final TagKey<Item> TAG_SPEARS = forge("tools/spears");

    private static TagKey<Item> forge(String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("forge", path));
    }

    public static boolean isPlayerHoldingWeapon(Player player) {
        return isWeapon(player.getMainHandItem()) || isWeapon(player.getOffhandItem());
    }
    
    private static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        
        if (stack.is(TAG_SWORDS)
                || stack.is(TAG_TRIDENTS)
                || stack.is(TAG_BOWS)
                || stack.is(TAG_CROSSBOWS)
                || stack.is(TAG_SPEARS)) {
            return true;
        }
        
        if (item instanceof TridentItem
                || item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof SwordItem) {
            return true;
        }
        
        Collection<AttributeModifier> damageModifiers =
        stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE);
        for (AttributeModifier modifier : damageModifiers) {
            if (modifier.getOperation() ==
            AttributeModifier.Operation.ADDITION && modifier.getAmount() > 4.0) {
                return true;
            }
        }

        return false;
    }
}