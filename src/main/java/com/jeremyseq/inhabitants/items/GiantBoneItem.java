package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public class GiantBoneItem extends SwordItem {
    public GiantBoneItem() {
        super(Tiers.NETHERITE, 11, -3.5f, new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        BogreEntity.shockwave(pPlayer);
        pPlayer.getCooldowns().addCooldown(this, 100);

        return super.use(pLevel, pPlayer, pUsedHand);
    }

    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return true;
    }
}
