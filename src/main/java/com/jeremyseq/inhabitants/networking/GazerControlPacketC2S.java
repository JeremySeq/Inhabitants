package com.jeremyseq.inhabitants.networking;

import com.jeremyseq.inhabitants.entities.gazer.GazerEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GazerControlPacketC2S {
    private final int gazerId;
    private final boolean forward;
    private final boolean back;
    private final boolean left;
    private final boolean right;
    private final boolean jump;

    public GazerControlPacketC2S(int gazerId, boolean forward, boolean back, boolean left, boolean right, boolean jump) {
        this.gazerId = gazerId;
        this.forward = forward;
        this.back = back;
        this.left = left;
        this.right = right;
        this.jump = jump;
    }

    public static void encode(GazerControlPacketC2S msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.gazerId);
        buf.writeBoolean(msg.forward);
        buf.writeBoolean(msg.back);
        buf.writeBoolean(msg.left);
        buf.writeBoolean(msg.right);
        buf.writeBoolean(msg.jump);
    }

    public static GazerControlPacketC2S decode(FriendlyByteBuf buf) {
        int id = buf.readInt();
        boolean f = buf.readBoolean();
        boolean b = buf.readBoolean();
        boolean l = buf.readBoolean();
        boolean r = buf.readBoolean();
        boolean j = buf.readBoolean();
        return new GazerControlPacketC2S(id, f, b, l, r, j);
    }

    public static void handle(GazerControlPacketC2S msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return; // sanity check

            // Lookup gazer by ID in the player's world
            Entity e = player.level().getEntity(msg.gazerId);
            if (!(e instanceof GazerEntity gazer)) return;

            if (gazer.currentState != GazerEntity.GazerState.BEING_CONTROLLED) return;

            // Apply movement
            Vec3 motion = Vec3.ZERO;
            double speed = 0.3;

            if (msg.forward) motion = motion.add(0, 0, -speed);
            if (msg.back)    motion = motion.add(0, 0, speed);
            if (msg.left)    motion = motion.add(-speed, 0, 0);
            if (msg.right)   motion = motion.add(speed, 0, 0);
            if (msg.jump)    motion = motion.add(0, 0.5, 0);

            gazer.setDeltaMovement(motion);

            // Optional: update rotation based on player look direction
            gazer.setYRot(player.getYRot());
            gazer.setXRot(player.getXRot());
        });
        ctx.get().setPacketHandled(true);
    }
}
