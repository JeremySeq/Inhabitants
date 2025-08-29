package com.jeremyseq.inhabitants.networking;

import com.jeremyseq.inhabitants.entities.gazer.GazerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GazerCameraPacketS2C {
    private final int gazerId;
    private final boolean activate;

    public GazerCameraPacketS2C(int gazerId, boolean activate) {
        this.gazerId = gazerId;
        this.activate = activate;
    }

    public static void encode(GazerCameraPacketS2C packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.gazerId);
        buf.writeBoolean(packet.activate);
    }

    public static GazerCameraPacketS2C decode(FriendlyByteBuf buf) {
        return new GazerCameraPacketS2C(buf.readInt(), buf.readBoolean());
    }

    public static void handle(GazerCameraPacketS2C packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return; // sanity check

            GazerEntity gazerEntity = (GazerEntity) Minecraft.getInstance().level.getEntity(packet.gazerId);

            if (packet.activate) {
                // Switch camera to gazer
                assert gazerEntity != null;
                mc.setCameraEntity(gazerEntity);
            } else {
                // Switch camera back to player
                assert mc.player != null;
                mc.setCameraEntity(mc.player);
            }
        });
        context.setPacketHandled(true);
    }
}
