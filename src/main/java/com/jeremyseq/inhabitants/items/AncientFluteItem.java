package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.entities.zinger.ZingerEntity;
import com.jeremyseq.inhabitants.entities.zinger.ZingerManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class AncientFluteItem extends Item {
    public AncientFluteItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack pStack, @NotNull Level pLevel, @NotNull Entity pEntity, int pSlotId, boolean pIsSelected) {
        super.inventoryTick(pStack, pLevel, pEntity, pSlotId, pIsSelected);

        if (pIsSelected && pEntity instanceof Player player && !pLevel.isClientSide) {
            ZingerEntity zinger = getZingerOnTheWay(player);
            if (zinger != null) {
                Vec2 zingerPos = new Vec2((float) zinger.getX(), (float) zinger.getZ());
                Vec2 playerPos = new Vec2((float) player.getX(), (float) player.getZ());
                int dist = (int) Math.sqrt(playerPos.distanceToSqr(zingerPos));
                player.displayClientMessage(Component.literal("Your Zinger is on the way! (" + dist + " blocks away)"), true);
            } else if (this.getZingerNearby(player) != null) {
                player.displayClientMessage(Component.literal("Your Zinger is nearby. Right-click to send them home."), true);
            } else {
                player.displayClientMessage(Component.literal("Right-click to call your Zinger."), true);
            }
        }
    }

    /**
     * @return the Zinger that is currently targeting the player, or null if none is found.
     */
    private @Nullable ZingerEntity getZingerOnTheWay(Player player) {
        for (ZingerEntity zinger : ZingerManager.getOwnedZingers(player)) {
            if (zinger.getTarget() == player) {
                return zinger;
            }
        }
        return null;
    }

    private @Nullable ZingerEntity getZingerNearby(Player player) {
        for (ZingerEntity zinger : ZingerManager.getOwnedZingers(player)) {
            if (zinger.distanceToSqr(player) < 225) {
                return zinger;
            }
        }
        return null;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level pLevel, @NotNull Player pPlayer, @NotNull InteractionHand pUsedHand) {
        ItemStack stack = pPlayer.getItemInHand(pUsedHand);

        if (!pLevel.isClientSide) {

            if (this.getZingerOnTheWay(pPlayer) != null) {
                pPlayer.sendSystemMessage(Component.literal("Your Zinger is already on the way!"));
                return InteractionResultHolder.consume(stack);
            } else if (this.getZingerNearby(pPlayer) != null) {
                Objects.requireNonNull(this.getZingerNearby(pPlayer)).triggerReturnToNest();
                return InteractionResultHolder.consume(stack);
            }

            List<ZingerEntity> zingers = ZingerManager.getOwnedZingers(pPlayer);

            ZingerEntity nearest = null;
            double nearestDist = Double.MAX_VALUE;

            for (ZingerEntity z : zingers) {
                double dist = z.distanceToSqr(pPlayer);
                if (dist < nearestDist) {
                    nearest = z;
                    nearestDist = dist;
                }
            }

            if (nearest != null) {
                pPlayer.sendSystemMessage(Component.literal("Calling your Zinger..."));
                nearest.setTarget(pPlayer);
            } else {
                pPlayer.sendSystemMessage(Component.literal("You don't own any Zingers."));
            }
        }

        return InteractionResultHolder.consume(stack);
    }
}
