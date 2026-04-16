package com.jeremyseq.inhabitants.networking;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.networking.bogre.*;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetworking {
    private static final String PROTOCOL_VERSION = "1.0";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static <T> void sendToServer(T message) {
        CHANNEL.sendToServer(message);
    }

    public static <T> void sendToPlayer(T message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void register() {
        // Client → Server
        CHANNEL.registerMessage(
                packetId++,
                BogreSkillKeyframePacketC2S.class,
                BogreSkillKeyframePacketC2S::encode,
                BogreSkillKeyframePacketC2S::decode,
                BogreSkillKeyframePacketC2S::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                BogreRecipePacketC2S.class,
                BogreRecipePacketC2S::encode,
                BogreRecipePacketC2S::decode,
                BogreRecipePacketC2S::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                AscendPacketC2S.class,
                AscendPacketC2S::encode,
                AscendPacketC2S::decode,
                AscendPacketC2S::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                DrillDamagePacketC2S.class,
                DrillDamagePacketC2S::encode,
                DrillDamagePacketC2S::decode,
                DrillDamagePacketC2S::handle
        );

        // Server → Client
        CHANNEL.registerMessage(
                packetId++,
                ScreenShakePacketS2C.class,
                ScreenShakePacketS2C::encode,
                ScreenShakePacketS2C::decode,
                ScreenShakePacketS2C::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                ShockwaveParticlePacketS2C.class,
                ShockwaveParticlePacketS2C::encode,
                ShockwaveParticlePacketS2C::decode,
                ShockwaveParticlePacketS2C::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                TinnitusPacketS2C.class,
                TinnitusPacketS2C::encode,
                TinnitusPacketS2C::decode,
                TinnitusPacketS2C::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                BogreRecipePacketS2C.class,
                BogreRecipePacketS2C::encode,
                BogreRecipePacketS2C::decode,
                BogreRecipePacketS2C::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                JavelinBounceSyncPacketS2C.class,
                JavelinBounceSyncPacketS2C::encode,
                JavelinBounceSyncPacketS2C::decode,
                JavelinBounceSyncPacketS2C::handle
        );
    }
}