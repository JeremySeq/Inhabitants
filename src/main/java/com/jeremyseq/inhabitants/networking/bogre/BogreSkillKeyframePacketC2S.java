package com.jeremyseq.inhabitants.networking.bogre;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogreAi;
import com.jeremyseq.inhabitants.entities.bogre.skill.BogreSkills;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BogreSkillKeyframePacketC2S {
    private final int bogreId;
    private final String keyframeName;

    public BogreSkillKeyframePacketC2S(int bogreId, String keyframeName) {
        this.bogreId = bogreId;
        this.keyframeName = keyframeName;
    }

    public static void encode(BogreSkillKeyframePacketC2S packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.bogreId);
        buffer.writeUtf(packet.keyframeName);
    }

    public static BogreSkillKeyframePacketC2S decode(FriendlyByteBuf buffer) {
        return new BogreSkillKeyframePacketC2S(buffer.readInt(), buffer.readUtf());
    }

    public static void handle(BogreSkillKeyframePacketC2S packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                Entity entity = player.level().getEntity(packet.bogreId);
                if (entity instanceof BogreEntity bogre) {
                    if (bogre.getAIState() == BogreAi.State.SKILLING) {
                        BogreAi.SkillingState state = bogre.getCraftingState();
                        
                        if (state == BogreAi.SkillingState.CARVING) {
                            BogreSkills.CARVING.keyframeTriggered(bogre, packet.keyframeName);
                        } else if (state == BogreAi.SkillingState.TRANSFORMATION) {
                            BogreSkills.TRANSFORMATION.keyframeTriggered(bogre, packet.keyframeName);
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
