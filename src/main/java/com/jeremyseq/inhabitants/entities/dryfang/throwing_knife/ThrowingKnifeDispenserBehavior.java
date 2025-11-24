package com.jeremyseq.inhabitants.entities.dryfang.throwing_knife;

import com.jeremyseq.inhabitants.entities.ModEntities;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ThrowingKnifeDispenserBehavior extends DefaultDispenseItemBehavior {
    @Override
    protected @NotNull ItemStack execute(@NotNull BlockSource source, @NotNull ItemStack stack) {
        Level level = source.getLevel();
        Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);

        ThrowingKnifeProjectile entity = ModEntities.THROWING_KNIFE_PROJECTILE.get()
                .create(level);

        if (entity != null) {
            // Offset the spawn point slightly in front of the dispenser face
            Vec3 spawnOffset = new Vec3(
                    direction.getStepX() * 0.6D,
                    direction.getStepY() * 0.6D,
                    direction.getStepZ() * 0.6D
            );

            Vec3 pos = new Vec3(source.x(), source.y(), source.z()).add(spawnOffset);
            entity.setPos(pos);

            entity.shoot(
                    direction.getStepX(),
                    direction.getStepY(),
                    direction.getStepZ(),
                    1.5F,
                    6.0F
            );

            entity.pickup = AbstractArrow.Pickup.ALLOWED;

            level.addFreshEntity(entity);

            stack.shrink(1);
        }

        return stack;
    }
}
