package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.enchantments.ModEnchantments;
import com.jeremyseq.inhabitants.networking.*;
import com.jeremyseq.inhabitants.util.ClientHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.SlotAccess;

import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ForgeMod;

import org.jetbrains.annotations.*;
import java.util.*;
import java.util.function.Consumer;

public class SpikeDrillItem extends PickaxeItem {
    public static final int baseTemperatureMax = 120;
    public static final String TAG_TEMPERATURE = "inhabitants:drill_temperature";
    public static final String TAG_LAST_USED = "inhabitants:drill_last_used";
    
    private static final Map<UUID, Long> clientStartTicks = new HashMap<>();
    private static final Map<UUID, Long> clientLastUseTicks = new HashMap<>();

    private static final int ticksPerSecond = 20;
    private static final int maxMissTicks = 5;
    private static final int overheatCooldownTicks = 2 * ticksPerSecond;
    private static final int snowballCooldownTicks = ticksPerSecond;
    private static final int useDurationTicks = 60 * 60 * ticksPerSecond;
    
    private static final int drillPacketInterval = 2;

    public SpikeDrillItem(Properties properties) {
        super(Tiers.IRON, 1, -2.8F, properties);
    }

    @Override
    public boolean isValidRepairItem(
        @NotNull ItemStack toRepair,
        @NotNull ItemStack repair
    ) {
        return repair.is(ModItems.IMPALER_SPIKE.get()) ||
            super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public float getDestroySpeed(
        @NotNull ItemStack pStack,
        @NotNull BlockState pState
    ) {
        return 0.0F;
    }

    @Override
    public boolean canAttackBlock(
        @NotNull BlockState pState,
        @NotNull Level pLevel,
        @NotNull BlockPos pPos,
        @NotNull Player pPlayer
    ) {
        return false;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) { return UseAnim.CUSTOM; }

    @Override
    public boolean shouldCauseReequipAnimation(
        ItemStack oldStack,
        ItemStack newStack,
        boolean slotChanged
    ) {
        return oldStack.getItem() != newStack.getItem() || slotChanged;
    }

    @Override
    public boolean isCorrectToolForDrops(
        @NotNull ItemStack stack,
        @NotNull BlockState state
    ) {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {

            return EnchantmentHelper.getItemEnchantmentLevel(
                ModEnchantments.DIAMOND_TIP.get(), stack) > 0;
        }

        return super.isCorrectToolForDrops(stack, state);
    }

    public static int getTemperatureMax(ItemStack stack) {
        int level = EnchantmentHelper.getItemEnchantmentLevel(
            ModEnchantments.THERMAL_CAPACITY.get(), stack);

        return baseTemperatureMax * (1 + level);
    }

    public static int getTemperature(ItemStack stack) {
        if (stack.hasTag()) {
            assert stack.getTag() != null;
            return stack.getTag().getInt(TAG_TEMPERATURE);
        } else {
            return 0;
        }
    }

    public static void setTemperature(ItemStack stack, int temperature) {
        stack.getOrCreateTag().putInt(
            TAG_TEMPERATURE,
            Math.max(0, Math.min(getTemperatureMax(stack), temperature)));
    }

    public static void addTemperature(
        ItemStack stack,
        int amount,
        long gameTime
    ) {
        int current = getTemperature(stack);
        setTemperature(stack, current + amount);
        stack.getOrCreateTag().putLong(TAG_LAST_USED, gameTime);
    }

    @Override
    public void onUseTick(
        @NotNull Level level,
        @NotNull LivingEntity entity,
        @NotNull ItemStack stack,
        int count
    ) {
        if (entity instanceof Player player) {
            if (!level.isClientSide) {
                stack.getOrCreateTag()
                    .putLong(TAG_LAST_USED, level.getGameTime());
            }

            BlockHitResult hit = getPlayerPOVHitResult(
                level,
                player,
                ClipContext.Fluid.NONE
            );

            boolean overBlock = hit.getType() == HitResult.Type.BLOCK;
            double reach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
            boolean inReach = hit.getLocation()
                .distanceTo(player.getEyePosition()) <= reach;

            if (!overBlock || !inReach) {
                int missTicks = stack.getOrCreateTag().getInt("drill_missTicks") + 1;

                if (missTicks > maxMissTicks) {
                    player.releaseUsingItem();
                    stack.getOrCreateTag().putInt("drill_missTicks", 0);
                    return;
                } else {
                    stack.getOrCreateTag().putInt("drill_missTicks", missTicks);
                }
            } else {
                stack.getOrCreateTag().putInt("drill_missTicks", 0);
            }

            if (level.isClientSide) {
                clientStartTicks.putIfAbsent(player.getUUID(), level.getGameTime());
                clientLastUseTicks.put(player.getUUID(), level.getGameTime());

                int elapsed = useDurationTicks - count;
                if (elapsed % drillPacketInterval == 0 &&
                    player.getUUID().equals(ClientHooks.getLocalPlayerUUID())) {
                    ModNetworking.sendToServer(new DrillDamagePacketC2S());
                }
            }
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
        @NotNull Level level,
        @NotNull Player player,
        @NotNull InteractionHand hand
    ) {
        ItemStack drill = player.getItemInHand(hand);
        ItemStack otherHand = player.getItemInHand(hand == InteractionHand.MAIN_HAND ?
            InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        
        if (otherHand.getItem() == Items.SNOWBALL) {
            return InteractionResultHolder.fail(drill);
        }

        // overheat
        if (getTemperature(drill) >= getTemperatureMax(drill)) {
            player.getCooldowns().addCooldown(this, overheatCooldownTicks);
            if (!level.isClientSide) {
                player.hurt(level.damageSources().onFire(), 2.0f);

                level.playSound(null,
                    player.blockPosition(),
                    SoundEvents.LAVA_EXTINGUISH,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.0f
                );
            }

            player.displayClientMessage(
                Component.translatable("tooltip.inhabitants.spike_drill.overheated")
                .withStyle(ChatFormatting.RED), true);
            
            return InteractionResultHolder.fail(drill);
        }
        
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);

        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.fail(drill);
        }
        
        double reach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        if (hit.getLocation().distanceTo(player.getEyePosition()) > reach) {
            return InteractionResultHolder.fail(drill);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(drill);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
        return useDurationTicks;
    }

    @Override
    public void releaseUsing(
        @NotNull ItemStack stack,
        @NotNull Level level,
        @NotNull LivingEntity entityLiving,
        int timeLeft
    ) {
        super.releaseUsing(stack, level, entityLiving, timeLeft);
        
        if (!level.isClientSide) {
            DrillDamagePacketC2S.clearMomentum(entityLiving);
            stack.getOrCreateTag()
                .putLong(TAG_LAST_USED, level.getGameTime());
        }
    }

    @Override
    public void inventoryTick(
        @NotNull ItemStack stack,
        Level level,
        @NotNull Entity entity,
        int slotId,
        boolean isSelected
    ) {
        if (level.isClientSide) return;

        int temperature = getTemperature(stack);
        if (temperature > 0) {
            long lastUsed = stack.getOrCreateTag().getLong(TAG_LAST_USED);
            long currentTime = level.getGameTime();
            long elapsed = currentTime - lastUsed;

            if (elapsed >= 60 && (elapsed - 60) % 20 == 0) {
                setTemperature(stack, temperature - 1);
            }
        }
    }

    @Override
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
        SpikeDrillClient.initializeDrillClient(consumer);
    }

    public static float calculatingDrillSpeed(Player player, ItemStack stack, float partialTick) {
        if (player == null) return 0f;
        
        Long start = clientStartTicks.get(player.getUUID());
        Long last  = clientLastUseTicks.get(player.getUUID());
        
        if (start == null || last == null) return 0f;
        
        if (player.level().getGameTime() - last > 40) {
            clientStartTicks.remove(player.getUUID());
            return 0f;
        }

        float duration = (player.level().getGameTime() - start) + partialTick;
        return Math.min(1.0f, duration / (float) DrillDamagePacketC2S.RAMP_UP_TICKS);
    }
    
    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @Nullable Level level,
        @NotNull List<Component> tooltipComponents,
        @NotNull TooltipFlag isAdvanced
    ) {
        tooltipComponents.add(
            Component.translatable("tooltip.inhabitants.spike_drill.temperature",
            getTemperature(stack), getTemperatureMax(stack))
            .withStyle(getTemperature(stack) >= getTemperatureMax(stack) ?
            ChatFormatting.RED : ChatFormatting.GOLD)
        );
        tooltipComponents.add(
            Component.translatable("tooltip.inhabitants.spike_drill")
            .withStyle(ChatFormatting.GRAY)
        );
        tooltipComponents.add(Component.literal(" "));
    }

    @Override
    public boolean overrideStackedOnOther(
        ItemStack stack,
        Slot slot,
        ClickAction action,
        Player player
    ) {
        ItemStack other = slot.getItem();
        if (other.is(Items.SNOWBALL) && getTemperature(stack) > 0) {
            if (!player.getCooldowns().isOnCooldown(this)) {
                addTemperature(stack, -30, player.level().getGameTime());
                if (!player.isCreative()) other.shrink(1);

                player.level().playSound(null, player.blockPosition(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.0f);
                player.getCooldowns().addCooldown(this, 20);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean overrideOtherStackedOnMe(
            @NotNull ItemStack stack,
            @NotNull ItemStack other,
            @NotNull Slot slot,
            @NotNull ClickAction action,
            @NotNull Player player,
            @NotNull SlotAccess access
    ) {
        if (other.is(Items.SNOWBALL) && getTemperature(stack) > 0) {
            if (!player.getCooldowns().isOnCooldown(this)) {
                addTemperature(stack, -30, player.level().getGameTime());
                if (!player.isCreative()) other.shrink(1);

                player.level().playSound(null, player.blockPosition(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.0f);
                
                player.getCooldowns().addCooldown(this, 20);
                return true;
            }
        }
        return false;
    }

}
