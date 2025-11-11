package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.effects.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GiantBoneItem extends SwordItem {
    public GiantBoneItem() {
        super(Tiers.NETHERITE, 10, -3.5f, new Item.Properties().stacksTo(1).fireResistant());
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, Level level, @NotNull Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        if (selected && !player.hasEffect(MobEffects.DAMAGE_BOOST) && !player.hasEffect(ModEffects.IMMUNITY.get())) {
            MobEffectInstance current = player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (current == null || current.getAmplifier() < 1 || current.getDuration() <= 10) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, true, false, true));
            }
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.literal("This is kind of heavy... Might need a strength boost.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
