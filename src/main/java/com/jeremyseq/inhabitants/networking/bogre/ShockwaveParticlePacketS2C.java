package com.jeremyseq.inhabitants.networking.bogre;

import com.jeremyseq.inhabitants.particles.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3f;

import java.util.function.Supplier;

public class ShockwaveParticlePacketS2C {
    private final Vec3 position;
    private final int lifetime;
    private final float maxScale;

    public ShockwaveParticlePacketS2C(Vec3 position, int lifetime, float maxScale) {
        this.position = position;
        this.lifetime = lifetime;
        this.maxScale = maxScale;
    }

    public static void encode(ShockwaveParticlePacketS2C packet, FriendlyByteBuf buf) {
        buf.writeVector3f(packet.position.toVector3f());
        buf.writeInt(packet.lifetime);
        buf.writeFloat(packet.maxScale);
    }

    public static ShockwaveParticlePacketS2C decode(FriendlyByteBuf buf) {
        Vector3f pos = buf.readVector3f();
        return new ShockwaveParticlePacketS2C(new Vec3(pos.x, pos.y, pos.z), buf.readInt(), buf.readFloat());
    }

    public static void handle(ShockwaveParticlePacketS2C packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            assert Minecraft.getInstance().level != null;
            Minecraft.getInstance().level.addParticle(
                    ModParticles.SHOCKWAVE.get(),
                    packet.position.x, packet.position.y, packet.position.z,
                    packet.lifetime, packet.maxScale, 0
            );

        });
        context.setPacketHandled(true);
    }
}
