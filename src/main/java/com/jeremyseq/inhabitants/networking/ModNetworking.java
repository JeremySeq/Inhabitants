package com.jeremyseq.inhabitants.networking;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
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

    public static void register() {
        // Client → Server
        CHANNEL.registerMessage(
                packetId++,
                GazerControlPacketC2S.class,
                GazerControlPacketC2S::encode,
                GazerControlPacketC2S::decode,
                GazerControlPacketC2S::handle
        );

        // Server → Client
        CHANNEL.registerMessage(
                packetId++,
                GazerCameraPacketS2C.class,
                GazerCameraPacketS2C::encode,
                GazerCameraPacketS2C::decode,
                GazerCameraPacketS2C::handle
        );
    }
}