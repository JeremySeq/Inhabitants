package com.jeremyseq.inhabitants.networking;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.enchantments.ModEnchantments;
import com.jeremyseq.inhabitants.items.SpikeDrillItem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.*;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID)
public class DrillDamagePacketC2S {
    
    private static final float DRILL_SPEED_MIN = 1.5f;
    private static final float DRILL_SPEED_MAX = 12.5f;
    private static final float DRILL_SPEED_MAX_DIAMOND_TIP = 25.0f;
    private static final float DRILL_SPEED_BONUS_FULL_MOMENTUM = 75.0f;

    private static final int ticksPerSec = 20;
    private static final int overheatCooldownTicks = 2 * ticksPerSec;
    private static final int elapsedCapTicks = ticksPerSec;
    private static final int elapsedDefaultTicks = 1;

    private static final float destroyDivisorCorrect = 30.0f;
    private static final float destroyDivisorWrong = 100.0f;
    private static final float drillContinuousDelay = 2.25f;

    public static final int RAMP_UP_TICKS = 15 * ticksPerSec;


    private static class MiningData {
        public BlockPos pos;
        public float progress;
        public long lastTick;
        public long startTick;
    }

    private static final Map<UUID, MiningData> MINING_DATA = new HashMap<>();

    public DrillDamagePacketC2S() {}

    public static void encode(DrillDamagePacketC2S packet, FriendlyByteBuf buffer) {
    }

    public static DrillDamagePacketC2S decode(FriendlyByteBuf buffer) {
        return new DrillDamagePacketC2S();
    }

    public static void handle(
        DrillDamagePacketC2S packet,
        Supplier<NetworkEvent.Context> ctx
    ) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ItemStack stack = player.getUseItem();
                if (stack.getItem() instanceof SpikeDrillItem) {
                    handleDamage(player);
                } else {
                    clearMomentum(player);
                }
            }
        });

        ctx.get().setPacketHandled(true);
    }

    private static void handleDamage(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

                double blockReach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
                HitResult rayTrace = player.pick(blockReach, 1.0f, false);
                if (!(rayTrace instanceof BlockHitResult bhr)) {
                    return;
                }

                BlockPos pos = bhr.getBlockPos();

                BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    float hardness = state.getDestroySpeed(level, pos);
                    if (hardness < 0) return; // unbreakable

                    MiningData data = MINING_DATA.computeIfAbsent(player.getUUID(), k -> {
                        MiningData d = new MiningData();
                        d.startTick = level.getGameTime();
                        d.lastTick = level.getGameTime();
                        return d;
                    });

                    if (!pos.equals(data.pos)) {
                        if (data.pos != null) {
                            level.destroyBlockProgress(
                                player.getId() + 1000000,
                                data.pos, -1
                            );
                        }

                        data.pos = pos;
                        data.progress = 0f;
                    }

                    ItemStack drill = player.getUseItem();
                    boolean isPickaxeMineable = state.is(BlockTags.MINEABLE_WITH_PICKAXE);

                    long duration = level.getGameTime() - data.startTick;
                    float ratio = Math.min(1.0f, (float) duration / RAMP_UP_TICKS);

                    float tickProgress = calculateDrillSpeed(
                        player, drill,
                        state, hardness, ratio);

                    long elapsed = level.getGameTime() - data.lastTick;

                    if (elapsed <= 0 || elapsed > elapsedCapTicks) elapsed =
                        elapsedDefaultTicks;
                    data.lastTick = level.getGameTime();

                    float addedProgress = tickProgress * elapsed;
                    data.progress += addedProgress;

                    if (data.progress >= 1.0f) {
                        processBlockBreak(player, level, pos, drill, data);
                        data.pos = null;

                    } else {
                        int crackLevel = (int) (data.progress * 10.0F);
                        level.destroyBlockProgress(player.getId() + 1000000, pos, crackLevel);
                    }
                }
    }

    public static void clearMomentum(LivingEntity entity) {
        if (entity == null) return;
        MiningData data = MINING_DATA.remove(entity.getUUID());
        
        if (data != null && data.pos != null &&
            entity.level() instanceof ServerLevel sl) {
            sl.destroyBlockProgress(entity.getId() + 1000000, data.pos, -1);
        }
    }

    private static float calculateDrillSpeed(
        ServerPlayer player,
        ItemStack drill,
        BlockState state,
        float hardness,
        float ratio
    ) {
        boolean isPickaxeMineable = state.is(BlockTags.MINEABLE_WITH_PICKAXE);
        boolean hasDiamondTip = EnchantmentHelper.getItemEnchantmentLevel(
                ModEnchantments.DIAMOND_TIP.get(), drill) > 0;
        
        float baseSpeed = isPickaxeMineable ? DRILL_SPEED_MIN : 1.0f;
        float maxSpeed  = isPickaxeMineable ? (hasDiamondTip ?
            DRILL_SPEED_MAX_DIAMOND_TIP : DRILL_SPEED_MAX) : 1.0f;


        
        int eff = EnchantmentHelper.getItemEnchantmentLevel(
            Enchantments.BLOCK_EFFICIENCY, drill);
        if (maxSpeed > 1.0F && eff > 0) {
            maxSpeed += (float)(eff * eff + 1);
        }

        if (MobEffectUtil.hasDigSpeed(player)) {
            maxSpeed *= 1.0F + (float)
                (MobEffectUtil.getDigSpeedAmplification(player) + 1) * 0.2F;
        }
        float speed = Mth.lerp(ratio, baseSpeed, maxSpeed);
        
        if (ratio >= 1.0f) {
            speed += DRILL_SPEED_BONUS_FULL_MOMENTUM;
        }

        if (player.hasEffect(MobEffects.DIG_SLOWDOWN)) {
            float fatigue = switch (Objects.requireNonNull(
                player.getEffect(MobEffects.DIG_SLOWDOWN)).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };
            speed *= fatigue;
        }

        if (player.isEyeInFluid(FluidTags.WATER) &&
            !EnchantmentHelper.hasAquaAffinity(player)) {
            speed /= 5.0F;
        }
        if (!player.onGround()) {
            speed /= 5.0F;
        }

        if (hardness <= 0.01f) {
            return 1.0f;
        }

        boolean canVanillaDrop = drill.isCorrectToolForDrops(state) ||
            !state.requiresCorrectToolForDrops();
        float vanillaTickProgress = speed / hardness / (canVanillaDrop ?
            destroyDivisorCorrect : destroyDivisorWrong);
        
        float vanillaTicksToBreak = 1.0f / vanillaTickProgress;
        float continuousTicksToBreak = vanillaTicksToBreak + drillContinuousDelay;

        return 1.0f / continuousTicksToBreak;
    }

    private static void processBlockBreak(
        ServerPlayer player,
        ServerLevel level,
        BlockPos pos,
        ItemStack drill,
        MiningData data
    ) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;

        SoundType blockSound = state.getSoundType(level, pos, player);
        level.playSound(null, pos,
            blockSound.getBreakSound(), SoundSource.BLOCKS,
            (blockSound.getVolume() + 1.0F) / 2.0F,
            blockSound.getPitch() * 0.8F);
        
        level.levelEvent(null,
            LevelEvent.PARTICLES_DESTROY_BLOCK,
            pos, Block.getId(state));

        boolean canHarvest = !state.requiresCorrectToolForDrops() ||
            drill.isCorrectToolForDrops(state);
        
        if (canHarvest) {
            Block.dropResources(state, level, pos, level.getBlockEntity(pos), player, drill);
        }
        
        level.removeBlock(pos, false);
        level.destroyBlockProgress(player.getId() + 1000000, pos, -1);
        data.progress = 0f;

        if (drill.getItem() instanceof SpikeDrillItem) {
            SpikeDrillItem.addTemperature(drill, 1, level.getGameTime());

            if (SpikeDrillItem.getTemperature(drill) >=
                SpikeDrillItem.getTemperatureMax(drill)) {
                
                player.hurt(level.damageSources().onFire(), 2.0f);
                player.releaseUsingItem();
                player.getCooldowns()
                    .addCooldown(drill.getItem(), overheatCooldownTicks);

                player.displayClientMessage(
                    Component.translatable("tooltip.inhabitants.spike_drill.overheated")
                    .withStyle(ChatFormatting.RED), true);

                level.playSound(
                    null, player.blockPosition(),
                    SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS,
                    1.0f, 1.0f
                );
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isServer() && event.phase == TickEvent.Phase.END) {
            MiningData data = MINING_DATA.get(event.player.getUUID());

            if (data != null && data.pos != null) {
                if (event.player.level().getGameTime() - data.lastTick > 40) {
                    if (event.player.level() instanceof ServerLevel sl) {
                        sl.destroyBlockProgress(event.player.getId() + 1000000, data.pos, -1);
                    }

                    MINING_DATA.remove(event.player.getUUID());
                }
            }
        }
    }
}
