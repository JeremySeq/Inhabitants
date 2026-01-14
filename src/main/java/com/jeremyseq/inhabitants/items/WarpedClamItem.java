package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.entities.ModEntities;
import com.jeremyseq.inhabitants.entities.warped_clam.WarpedClamEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class WarpedClamItem extends Item {
    public WarpedClamItem(Properties pProperties) {
        super(pProperties.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            Player player = context.getPlayer();
            BlockPos clickedPos = context.getClickedPos();
            Direction face = context.getClickedFace();
            ItemStack itemStack = context.getItemInHand();

            double x = clickedPos.getX() + 0.5;
            double y = clickedPos.getY() + 0.5 + (face == Direction.UP ? 0.5 : 0.0);
            double z = clickedPos.getZ() + 0.5;

            WarpedClamEntity clam = new WarpedClamEntity(ModEntities.WARPED_CLAM.get(), level);
            clam.setPos(x, y, z);

            // face the same direction the player is looking
            float yaw = context.getRotation();
            int direction = Math.round(yaw / 45f) & 7;
            clam.setDir(direction);
            clam.updateRot();

            if (itemStack.getTag() != null && itemStack.getTag().contains("has_pearl")) {
                clam.setHasPearl(itemStack.getTag().getBoolean("has_pearl"));
            } else {
                clam.setHasPearl(false);
            }

            level.addFreshEntity(clam);

            level.playSound(null, x, y, z, SoundEvents.MUD_PLACE, clam.getSoundSource(), 1.0f, 1.0f);

            if (player != null && !player.getAbilities().instabuild) {
                itemStack.shrink(1);
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return InteractionResult.SUCCESS;
    }
}
