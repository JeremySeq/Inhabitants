package com.jeremyseq.inhabitants.networking;

import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record TinnitusPacketS2C() {

    public static void encode(TinnitusPacketS2C packet, FriendlyByteBuf buf) {}

    public static TinnitusPacketS2C decode(FriendlyByteBuf buf) {
        return new TinnitusPacketS2C();
    }

    public static void handle(TinnitusPacketS2C packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(ClientHandler::playTinnitus);
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static class ClientHandler {
        public static void playTinnitus() {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(ModSoundEvents.IMPALER_CONCUSSION.get(), 1.0f));
        }
    }
}