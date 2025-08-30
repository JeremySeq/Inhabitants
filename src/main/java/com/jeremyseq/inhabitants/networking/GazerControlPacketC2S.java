package com.jeremyseq.inhabitants.networking;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.entities.gazer.GazerEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GazerControlPacketC2S {
    private final int gazerId;
    private final boolean forward;
    private final boolean back;
    private final boolean left;
    private final boolean right;
    private final boolean jump;
    private final boolean sneak;
    private final float yaw;
    private final float pitch;

    public GazerControlPacketC2S(int gazerId, boolean forward, boolean back, boolean left, boolean right, boolean jump, boolean sneak, float yaw, float pitch) {
        this.gazerId = gazerId;
        this.forward = forward;
        this.back = back;
        this.left = left;
        this.right = right;
        this.jump = jump;
        this.sneak = sneak;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static void encode(GazerControlPacketC2S msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.gazerId);
        buf.writeBoolean(msg.forward);
        buf.writeBoolean(msg.back);
        buf.writeBoolean(msg.left);
        buf.writeBoolean(msg.right);
        buf.writeBoolean(msg.jump);
        buf.writeBoolean(msg.sneak);
        buf.writeFloat(msg.yaw);
        buf.writeFloat(msg.pitch);
    }

    public static GazerControlPacketC2S decode(FriendlyByteBuf buf) {
        int id = buf.readInt();
        boolean forward = buf.readBoolean();
        boolean back = buf.readBoolean();
        boolean left = buf.readBoolean();
        boolean right = buf.readBoolean();
        boolean jump = buf.readBoolean();
        boolean sneak = buf.readBoolean();
        float yaw = buf.readFloat();
        float pitch = buf.readFloat();
        return new GazerControlPacketC2S(id, forward, back, left, right, jump, sneak, yaw, pitch);
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

//            if (msg.forward) motion = motion.add(0, 0, -speed);
//            if (msg.back)    motion = motion.add(0, 0, speed);
//            if (msg.left)    motion = motion.add(-speed, 0, 0);
//            if (msg.right)   motion = motion.add(speed, 0, 0);
//            if (msg.jump)    motion = motion.add(0, 0.5, 0);
//            if (msg.sneak)   motion = motion.add(0, -0.5, 0);

//            gazer.setDeltaMovement(motion);

            // Set rotation from packet
            gazer.setYRot(msg.yaw);
            gazer.setXRot(msg.pitch);
            gazer.setYHeadRot(msg.yaw);

            moveRelativeToYaw(gazer, msg.yaw, msg.forward, msg.back, msg.left, msg.right, msg.jump, msg.sneak, speed);

            Inhabitants.LOGGER.debug("Gazer {} controlled by player {}: pos=({}, {}, {}), rot=({}, {})",
                    gazer.getId(), player.getGameProfile().getName(),
                    gazer.getX(), gazer.getY(), gazer.getZ(),
                    gazer.getYRot(), gazer.getXRot());
        });
        ctx.get().setPacketHandled(true);
    }

    public static void moveRelativeToYaw(Entity entity, float yaw, boolean forward, boolean back, boolean left, boolean right, boolean jump, boolean sneak, double speed) {
        float dx = 0, dz = 0;
        if (forward) dz += 1;
        if (back) dz -= 1;
        if (left) dx += 1;
        if (right) dx -= 1;

        float length = (float) Math.sqrt(dx * dx + dz * dz);
        if (length > 0) {
            dx /= length;
            dz /= length;
        }

        float yawRad = (float) Math.toRadians(yaw);
        float sin = (float) Math.sin(yawRad);
        float cos = (float) Math.cos(yawRad);

        float worldX = dx * cos - dz * sin;
        float worldZ = dz * cos + dx * sin;

        double worldY;
        if (jump) worldY = 0.5;
        else if (sneak) worldY = -0.5;
        else worldY = 0;

        entity.setDeltaMovement(worldX * speed, worldY, worldZ * speed);
    }
}
