package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.entities.ModEntities;
import com.jeremyseq.inhabitants.entities.gazer.GazerEntity;
import com.jeremyseq.inhabitants.networking.GazerCameraPacketS2C;
import com.jeremyseq.inhabitants.networking.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;

import javax.annotation.Nullable;
import java.util.List;

public class GazerPodItem extends Item {
//    public GazerEntity gazerEntity = null; // The currently controlled gazer

    public GazerPodItem(Properties properties) {
        super(properties);
    }

    // ===== NBT Helpers =====
    public static boolean hasGazer(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean("HasGazer");
    }

    public static void setHasGazer(ItemStack stack, boolean hasGazer) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean("HasGazer", hasGazer);
    }

    public static int getGazerId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return -1;
        return tag.getInt("GazerId") != 0 ? tag.getInt("GazerId") : -1;
    }

    public static void setGazerId(ItemStack stack, int gazerId) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("GazerId", gazerId);
    }

    // ===== Right Click in Air =====
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // called on server when releasing
        if (!level.isClientSide && hasGazer(stack) && player instanceof ServerPlayer serverPlayer) {
            // Release gazer
            GazerEntity gazerEntity = ModEntities.GAZER.get().create(level);

            assert gazerEntity != null;
            gazerEntity.podOwner = player.getUUID();
            gazerEntity.moveTo(player.getX(), player.getY() + 1, player.getZ(), player.getYRot(), player.getXRot());
            level.addFreshEntity(gazerEntity);

            setGazerId(stack, gazerEntity.getId());

            gazerEntity.exitPod(player, true);

            setHasGazer(stack, false);

            Inhabitants.LOGGER.debug("Releasing gazer with ID {}", gazerEntity.getId());

            ModNetworking.CHANNEL.sendTo(new GazerCameraPacketS2C(gazerEntity.getId(), true),
                    serverPlayer.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT);

            return InteractionResultHolder.success(stack);
        }
        // called on client to start control gazer
//        if (level.isClientSide && player == Minecraft.getInstance().player) {
//            GazerEntity gazerEntity = (GazerEntity) level.getEntity(getGazerId(stack));
//            assert gazerEntity != null;
//            if (gazerEntity.currentState != GazerEntity.GazerState.BEING_CONTROLLED || gazerEntity.podOwner == null || !gazerEntity.podOwner.equals(player.getUUID())) {
//                // Can't control a gazer that's not in the right state or has an owner already
//                return super.use(level, player, hand);
//            }
//
//            Minecraft mc = Minecraft.getInstance();
//
//            // switch camera to gazer
//            Inhabitants.LOGGER.debug("Switching camera to gazer");
//            if (mc.getCameraEntity() != gazerEntity) {
//                mc.setCameraEntity(gazerEntity);
//            }
//
//            // Start sending input to the server each tick
//            // ControlClientTickEventHandler.startSendingInput();
//            return InteractionResultHolder.success(stack);
//        }

        return super.use(level, player, hand);
    }

    // ===== Right Click Entity =====
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!player.level().isClientSide && target instanceof GazerEntity gazer) {
            if (!hasGazer(stack)) {
                // Capture gazer
                setHasGazer(stack, true);
                gazer.enterPod(); // calls discard()
                return InteractionResult.SUCCESS;
            }
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    // ===== Tooltip =====
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (hasGazer(stack)) {
            tooltip.add(net.minecraft.network.chat.Component.literal("Contains a Gazer"));
        } else {
            tooltip.add(net.minecraft.network.chat.Component.literal("Empty Pod"));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
