package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.entities.ModEntities;
import com.jeremyseq.inhabitants.entities.projectiles.ImpalerSpikeProjectile;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ImpalerSpikeItem extends ArrowItem {
    public ImpalerSpikeItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        return new ImpalerSpikeProjectile(ModEntities.IMPALER_SPIKE_PROJECTILE.get(), shooter, level);
    }
}
