package com.jeremyseq.inhabitants.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BracerOfMightItem extends Item {
    public BracerOfMightItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, Level level, @NotNull Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;

        // Only apply if item is in hotbar (slots 0–8)
        if (slot < 9) {
            MobEffectInstance current = player.getEffect(MobEffects.DAMAGE_BOOST);
            if (current == null || current.getAmplifier() < 1 || current.getDuration() <= 10) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 1, true, false, true));
            }
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.literal("Grants Strength II while in hotbar").withStyle(ChatFormatting.BLUE));
    }
}
