package com.jeremyseq.inhabitants.networking;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.entities.gazer.GazerEntity;
import com.jeremyseq.inhabitants.items.GazerPodItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class GazerStartControlC2S {
    private final int gazerId;

    public GazerStartControlC2S(int gazerId) {
        this.gazerId = gazerId;
    }

    public static void encode(GazerStartControlC2S msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.gazerId);
    }

    public static GazerStartControlC2S decode(FriendlyByteBuf buf) {
        int id = buf.readInt();
        return new GazerStartControlC2S(id);
    }

    public static void handle(GazerStartControlC2S msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!(helmet.getItem() instanceof GazerPodItem)) return;

            GazerEntity gazer = player.level().getEntity(msg.gazerId) instanceof GazerEntity g ? g : null;
            if (gazer == null) return;

            if (gazer.currentState != GazerEntity.GazerState.IDLE) return;

            if (gazer.podOwner != player.getUUID()) return;

            // set controlled state and owner
            gazer.currentState = GazerEntity.GazerState.BEING_CONTROLLED;

            ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new GazerCameraPacketS2C(gazer.getId(), true));
            Inhabitants.LOGGER.debug("Player {} started controlling Gazer {}", player.getName().getString(), gazer.getId());
        });
        ctx.get().setPacketHandled(true);
    }
}
