package com.jeremyseq.inhabitants.items;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GiantBoneItem extends SwordItem {
    public GiantBoneItem() {
        super(Tiers.NETHERITE, 7, -3.5f, new Item.Properties().stacksTo(1));
    }

    private static final float SHOCKWAVE_RADIUS = 9;
    private static final float SHOCKWAVE_DAMAGE = 20f;
    private static final int SHOCKWAVE_DURATION = 40;
    private static final int COOLDOWN = 100;

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("  "));
        tooltip.add(Component.translatable("tooltip.inhabitants.special_effect").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.literal("  • ").append(Component.translatable("tooltip.inhabitants.giant_bone")).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, Level level, @NotNull Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        if (selected && !player.hasEffect(MobEffects.DAMAGE_BOOST)) {
            MobEffectInstance current = player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (current == null || current.getAmplifier() < 1 || current.getDuration() <= 10) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, true, false, true));
            }
        }
    }

    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return true;
    }

    public static float getShockwaveRadius() { return SHOCKWAVE_RADIUS; }
    public static float getShockwaveDamage() { return SHOCKWAVE_DAMAGE; }
    public static int getShockwaveDuration() { return SHOCKWAVE_DURATION; }
    public static int getCooldown() { return COOLDOWN; }
}
