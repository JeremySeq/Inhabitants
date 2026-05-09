package com.jeremyseq.inhabitants.networking;

import com.jeremyseq.inhabitants.entities.javelin.JavelinEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class JavelinBounceTriggerPacketC2S {
    private final int javelinId;

    public JavelinBounceTriggerPacketC2S(int javelinId) {
        this.javelinId = javelinId;
    }

    public static void encode(JavelinBounceTriggerPacketC2S msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.javelinId);
    }

    public static JavelinBounceTriggerPacketC2S decode(FriendlyByteBuf buffer) {
        return new JavelinBounceTriggerPacketC2S(buffer.readInt());
    }

    public static void handle(JavelinBounceTriggerPacketC2S msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();

            if (player != null) {
                Entity entity = player.level().getEntity(msg.javelinId);
                if (entity instanceof JavelinEntity javelin) {
                    
                    if (player.distanceToSqr(javelin) < 9.0) {
                        javelin.serverLaunch(player);
                    }
                }
            }
        });
        
        ctx.setPacketHandled(true);
    }
}
