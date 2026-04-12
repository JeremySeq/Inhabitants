package com.jeremyseq.inhabitants.entities.bogre.utilities;

import com.jeremyseq.inhabitants.entities.EntityUtil;
import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogreAi;
import com.jeremyseq.inhabitants.entities.bogre.ai.ShockwaveGoal;
import com.jeremyseq.inhabitants.items.ModItems;

import net.minecraft.tags.TagKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.List;

public class BogreUtil {
    
    // weapons tags
    private static final TagKey<Item> TAG_SWORDS = forge("tools/swords");
    private static final TagKey<Item> TAG_TRIDENTS = forge("tools/tridents");
    private static final TagKey<Item> TAG_BOWS = forge("tools/bows");
    private static final TagKey<Item> TAG_CROSSBOWS = forge("tools/crossbows");
    private static final TagKey<Item> TAG_SPEARS = forge("tools/spears");

    private static TagKey<Item> forge(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", path));
    }

    public static boolean isPlayerHoldingWeapon(Player player) {
        return isWeapon(player.getMainHandItem()) ||
        isWeapon(player.getOffhandItem());
    }
    
    private static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();

        if (stack.is(ModItems.GIANT_BONE.get())) return false;
        
        if (stack.is(TAG_SWORDS) || stack.is(TAG_TRIDENTS) ||
        stack.is(TAG_BOWS) || stack.is(TAG_CROSSBOWS) ||
        stack.is(TAG_SPEARS)) {
            return true;
        }
        
        if (item instanceof TridentItem || item instanceof BowItem ||
        item instanceof CrossbowItem || item instanceof SwordItem) {
            return true;
        }
        
        Collection<AttributeModifier> damageModifiers =
        stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE);

        for (AttributeModifier modifier : damageModifiers) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION &&
            modifier.getAmount() > 4.0) {
                return true;
            }
        }
        return false;
    }

    // --- Helpers ---

    public static void triggerShockwave(BogreEntity bogre, float damage, float radius) {
        if (bogre.level().isClientSide) return;
        
        Vec3 forward = bogre.getLookAngle().normalize().scale(3.0);
        Vec3 spawnPos = bogre.position().add(forward.x, 0, forward.z);
        
        ShockwaveGoal.addShockwave((ServerLevel) bogre.level(), spawnPos, damage, radius, 40, bogre);
    }

    public static void throwHeldItem(BogreEntity bogre) {
        BogreAi.playAnimation(bogre, "grab");
        EntityUtil.throwItemStack(bogre.level(), bogre, bogre.getItemHeld(), .3f, 0);
        bogre.setItemHeld(ItemStack.EMPTY, false);
    }

    public static BlockPos getAveragePosition(List<BlockPos> positions) {
        if (positions == null || positions.isEmpty()) return BlockPos.ZERO;
        int x = 0, y = 0, z = 0;
        for (BlockPos pos : positions) {
            x += pos.getX();
            y += pos.getY();
            z += pos.getZ();
        }
        return new BlockPos(x / positions.size(), y / positions.size(), z / positions.size());
    }
}