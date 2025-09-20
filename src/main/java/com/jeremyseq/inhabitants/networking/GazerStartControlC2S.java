package com.jeremyseq.inhabitants.networking;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.entities.gazer.GazerEntity;
import com.jeremyseq.inhabitants.items.GazerPodItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

public class GazerStartControlC2S {
    private final UUID gazerId;

    public GazerStartControlC2S(UUID gazerId) {
        this.gazerId = gazerId;
    }

    public static void encode(GazerStartControlC2S msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.gazerId);
    }

    public static GazerStartControlC2S decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        return new GazerStartControlC2S(id);
    }

    public static void handle(GazerStartControlC2S msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!(helmet.getItem() instanceof GazerPodItem)) return;

            GazerEntity gazer = player.serverLevel().getEntity(msg.gazerId) instanceof GazerEntity g ? g : null;
            if (gazer == null) return;

            if (gazer.isEnteringPod()) return;

            if (gazer.getOwnerUUID() != player.getUUID()) return;

            // set controlled state and owner
            gazer.setGazerState(GazerEntity.GazerState.BEING_CONTROLLED);

            ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new GazerCameraPacketS2C(gazer.getId(), true));
            Inhabitants.LOGGER.debug("Player {} started controlling Gazer {}", player.getName().getString(), gazer.getUUID());
        });
        ctx.get().setPacketHandled(true);
    }
}
