package com.jeremyseq.inhabitants.effects;

import com.jeremyseq.inhabitants.Inhabitants;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerEvent;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID)
public class ReverseGrowthEvents {

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEffectInstance().getEffect() == ModEffects.REVERSE_GROWTH.get()) {
            LivingEntity entity = event.getEntity();
            if (entity instanceof Player player) {
                player.refreshDimensions();
            }
            
            if (!entity.level().isClientSide) {
                ServerLevel serverLevel = (ServerLevel) entity.level();

                ClientboundUpdateMobEffectPacket packet =
                    new ClientboundUpdateMobEffectPacket(entity.getId(), event.getEffectInstance());

                serverLevel.getChunkSource().broadcast(entity, packet);
            }
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null
                && event.getEffectInstance().getEffect() == ModEffects.REVERSE_GROWTH.get()) {
            LivingEntity entity = event.getEntity();
            if (entity instanceof Player player) {
                player.refreshDimensions();
            }

            if (!entity.level().isClientSide) {
                ServerLevel serverLevel = (ServerLevel) entity.level();

                ClientboundRemoveMobEffectPacket packet =
                    new ClientboundRemoveMobEffectPacket(entity.getId(), ModEffects.REVERSE_GROWTH.get());
                
                serverLevel.getChunkSource().broadcast(entity, packet);
            }
        }
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEffect() == ModEffects.REVERSE_GROWTH.get()) {
            LivingEntity entity = event.getEntity();
            if (entity instanceof Player player) {
                player.refreshDimensions();
            }

            if (!entity.level().isClientSide) {
                ServerLevel serverLevel = (ServerLevel) entity.level();

                ClientboundRemoveMobEffectPacket packet =
                    new ClientboundRemoveMobEffectPacket(entity.getId(), ModEffects.REVERSE_GROWTH.get());

                serverLevel.getChunkSource().broadcast(entity, packet);
            }
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof LivingEntity living && living.hasEffect(ModEffects.REVERSE_GROWTH.get())) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                var effect = living.getEffect(ModEffects.REVERSE_GROWTH.get());
                if (effect != null) {
                    serverPlayer.connection.send(new ClientboundUpdateMobEffectPacket(living.getId(), effect));
                }
            }
        }
    }
}
