package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.entities.nightmare.slash_projectile.SlashProjectile;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;

public class RiftbladeItem extends SwordItem {
    public RiftbladeItem() {
        super(Tiers.NETHERITE, 7, -1.0f, new Item.Properties().stacksTo(1));
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        SlashProjectile slashProjectile = new SlashProjectile(entity.level(), entity);
        entity.level().addFreshEntity(slashProjectile);
        return true;
    }
}
