package com.jeremyseq.inhabitants.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

public class ClientHooks {
    
    @OnlyIn(Dist.CLIENT)
    public static boolean isLocalPlayer(Player player) {
        return player == Minecraft.getInstance().player;
    }

    @OnlyIn(Dist.CLIENT)
    public static UUID getLocalPlayerUUID() {
        if (Minecraft.getInstance().player != null) {
            return Minecraft.getInstance().player.getUUID();
        }
        return null;
    }
}
