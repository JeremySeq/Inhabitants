package com.jeremyseq.inhabitants.networking;

import com.jeremyseq.inhabitants.events.ModEvents;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;

import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class JavelinBounceSyncPacketS2C {
    private final int combo;
    private final long lastBounceTick;

    public JavelinBounceSyncPacketS2C(int combo, long lastBounceTick) {
        this.combo = combo;
        this.lastBounceTick = lastBounceTick;
    }

    public static void encode(JavelinBounceSyncPacketS2C packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.combo);
        buf.writeLong(packet.lastBounceTick);
    }

    public static JavelinBounceSyncPacketS2C decode(FriendlyByteBuf buf) {
        return new JavelinBounceSyncPacketS2C(buf.readInt(), buf.readLong());
    }

    public static void handle(JavelinBounceSyncPacketS2C packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {

            Player player = Minecraft.getInstance().player;
            if (player != null) {

                ModEvents.BOUNCE_COMBOS.put(player.getUUID(), packet.combo);
                ModEvents.LAST_BOUNCE_TICKS.put(player.getUUID(), packet.lastBounceTick);
            }
        });
        
        context.setPacketHandled(true);
    }
}
