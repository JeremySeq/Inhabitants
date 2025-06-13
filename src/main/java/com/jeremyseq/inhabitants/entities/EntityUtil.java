package com.jeremyseq.inhabitants.entities;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class EntityUtil {
    public static void throwItemStack(Level level, Entity entity, ItemStack stack, float speed, float upwardBias) {
        if (level.isClientSide || stack.isEmpty()) return;

        // spawn the item
        ItemEntity itemEntity = new ItemEntity(
                level,
                entity.getX(),
                entity.getEyeY(),
                entity.getZ(),
                stack.copy()
        );

        // direction in front of the entity
        Vec3 look = entity.getLookAngle().normalize();
        Vec3 motion = look.scale(speed).add(0, upwardBias, 0);
        itemEntity.setDeltaMovement(motion);

        // set pickup delay like a player-thrown item
        itemEntity.setDefaultPickUpDelay();

        level.addFreshEntity(itemEntity);
    }

}
