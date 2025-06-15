package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;

public class GiantBoneItem extends SwordItem {

    public GiantBoneItem() {
        super(Tiers.NETHERITE, 11, -3.5f, new Item.Properties().stacksTo(1));
    }

    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return true;
    }

    @Override
    public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
        BogreEntity.shockwave(pAttacker);
        return super.hurtEnemy(pStack, pTarget, pAttacker);
    }
}