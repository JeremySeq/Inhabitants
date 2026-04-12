package com.jeremyseq.inhabitants.networking;

import com.jeremyseq.inhabitants.effects.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AscendPacketC2S {
    public AscendPacketC2S() {}

    public static void encode(AscendPacketC2S msg, FriendlyByteBuf buffer) {}

    public static AscendPacketC2S decode(FriendlyByteBuf buffer) {
        return new AscendPacketC2S();
    }

    // handles player ascend when they are inside a block
    public static void handle(AscendPacketC2S msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();

            if (player != null &&
                player.hasEffect(ModEffects.IMMATERIAL.get())) {

                if (isInsideBlock(player)) {
                    player.teleportTo(
                        player.getX(),
                        player.getY() + 1.0D,
                        player.getZ());
                }

            }
        });

        ctx.setPacketHandled(true);
    }

    // checks if the player is inside a block
    private static boolean isInsideBlock(ServerPlayer player) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        for (double dy : new double[]{0.1D, 0.8D, 1.62D}) {
            BlockPos pos = BlockPos.containing(px, py + dy, pz);

            if (!player.level().getBlockState(pos)
            .getCollisionShape(player.level(), pos, CollisionContext.empty()).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
