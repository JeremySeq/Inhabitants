package com.jeremyseq.inhabitants.networking;

import com.jeremyseq.inhabitants.screen_shake.CameraRenderEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ScreenShakePacketS2C {
    private final int durationTicks;

    public ScreenShakePacketS2C(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    public static void encode(ScreenShakePacketS2C packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.durationTicks);
    }

    public static ScreenShakePacketS2C decode(FriendlyByteBuf buf) {
        return new ScreenShakePacketS2C(buf.readInt());
    }

    public static void handle(ScreenShakePacketS2C packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> CameraRenderEvent.triggerShake(packet.durationTicks));
        context.setPacketHandled(true);
    }
}
