package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.entities.catcher.WaterberryProjectile;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class WaterberryItem extends Item {
    public static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(3)
            .saturationMod(0.4f)
            .build();

    public WaterberryItem(Properties props) {
        super(props.food(FOOD));
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // shift-right-click to eat
        if (player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }

        // right click air → throw
        if (!level.isClientSide) {
            WaterberryProjectile projectile = new WaterberryProjectile(level, player);
            projectile.setItem(stack.copy());
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(projectile);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
