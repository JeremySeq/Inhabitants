package com.jeremyseq.inhabitants.networking;

import com.jeremyseq.inhabitants.entities.gazer.GazerEntity;
import com.jeremyseq.inhabitants.items.GazerPodItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class GazerStopControlC2S {
    private final int gazerId;

    public GazerStopControlC2S(int gazerId) {
        this.gazerId = gazerId;
    }

    public static void encode(GazerStopControlC2S msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.gazerId);
    }

    public static GazerStopControlC2S decode(FriendlyByteBuf buf) {
        int id = buf.readInt();
        return new GazerStopControlC2S(id);
    }

    public static void handle(GazerStopControlC2S msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!(helmet.getItem() instanceof GazerPodItem)) return;


            GazerEntity gazer = player.level().getEntity(msg.gazerId) instanceof GazerEntity g ? g : null;
            if (gazer == null) return;

            // return if not the owner
            if (!player.getUUID().equals(gazer.getOwnerUUID())) return;

            gazer.setGazerState(GazerEntity.GazerState.IDLE);

            ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new GazerCameraPacketS2C(gazer.getId(), false));

        });
        ctx.get().setPacketHandled(true);
    }
}
