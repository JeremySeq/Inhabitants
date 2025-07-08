package com.jeremyseq.inhabitants.items.tickers;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID)
public class LassoTicker {

    // Map with user UUID and target UUID
    private static final Map<UUID, UUID> activeLassos = new HashMap<>();

    /** Start pulling a mob toward the user. Called from anywhere. */
    public static void startLassoing(Entity user, Mob target) {
        if (!activeLassos.containsValue(target)) {
            target.setLeashedTo(user, true);
            activeLassos.put(user.getUUID(), target.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();

        Iterator<Map.Entry<UUID, UUID>> iterator = activeLassos.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, UUID> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }


            if (player.serverLevel().getEntity(entry.getValue()) instanceof Mob mob) {
                if (mob.isDeadOrDying() || !mob.isLeashed()) {
                    iterator.remove();
                    continue;
                }

                Vec3 pull = player.position().subtract(mob.position()).scale(0.5);
                mob.setDeltaMovement(pull);

                if (mob.distanceTo(player) < 2) {
                    mob.dropLeash(true, true);
                    iterator.remove();
                }
            }

        }
    }
}