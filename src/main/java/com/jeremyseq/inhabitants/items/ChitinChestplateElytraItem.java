package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.items.armor.ModArmorMaterials;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;

public class ChitinChestplateElytraItem extends ArmorItem {
    public ChitinChestplateElytraItem() {
        super(
            ModArmorMaterials.CHITIN,
            Type.CHESTPLATE,
            new Properties()
                .stacksTo(1)
                .durability((int) (ModArmorMaterials.CHITIN.getDurabilityForType(Type.CHESTPLATE) * 1.5))
        );
    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return stack.getDamageValue() < stack.getMaxDamage() - 1;
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (!entity.level().isClientSide) {
            int nextFlightTick = flightTicks + 1;
            if (nextFlightTick % 10 == 0) {
                if (nextFlightTick % 20 == 0) {
                    stack.hurtAndBreak(1, entity, (e) -> e.broadcastBreakEvent(EquipmentSlot.CHEST));
                }

                entity.gameEvent(GameEvent.ELYTRA_GLIDE);
            }
        }

        return true;
    }

    public boolean isValidRepairItem(@NotNull ItemStack pToRepair, ItemStack pRepair) {
        return pRepair.is(Items.PHANTOM_MEMBRANE) || pRepair.is(ModItems.CHITIN.get());
    }

    public @NotNull SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_ELYTRA;
    }

    public @NotNull EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.CHEST;
    }
}
