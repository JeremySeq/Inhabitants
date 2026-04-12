package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.animation.FPVAnimationPlayer;
import com.jeremyseq.inhabitants.audio.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

import net.minecraftforge.api.distmarker.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class SpikeDrillClient {

    private static boolean wasOverheated = false;
    private static boolean initialized = false;

    public static void initializeDrillClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(
                    PoseStack poseStack,
                    LocalPlayer player,
                    HumanoidArm arm,
                    ItemStack itemInHand,
                    float partialTick,
                    float equipProcess,
                    float swingProcess
            ) {
                init(player);

                boolean isOverheated = SpikeDrillItem.getTemperature(itemInHand) >=
                        SpikeDrillItem.getTemperatureMax(itemInHand);
                
                if (isOverheated && !wasOverheated) {
                    FPVAnimationPlayer.INSTANCE.playOverridePhase(arm, "spike_drill_overheat");
                }

                wasOverheated = isOverheated;

                float ratio = SpikeDrillItem.calculatingDrillSpeed(player, itemInHand, partialTick);
                float speedMultiplier = 0.9f;
                
                if (ratio >= 0.1f) {
                    float alpha = (ratio - 0.1f) / 0.9f;
                    speedMultiplier = Mth.lerp(alpha, 0.9f, 1.5f);
                }

                FPVAnimationPlayer.INSTANCE.setSpeed(arm, speedMultiplier);

                return FPVAnimationPlayer.INSTANCE.apply("spike_drill_start",
                        poseStack,
                        player,
                        arm,
                        itemInHand,
                        partialTick,
                        equipProcess,
                        true
                );
            }
        });
    }

    private static void init(Player player) {
        if (initialized) return;
        initialized = true;

        FPVAnimationPlayer.INSTANCE.setTriggerCallback((id, p, arm, stack) -> {
            if ("inhabitants:drill_dig".equals(id)) {
                HitResult hit = Minecraft.getInstance().hitResult;

                if (hit instanceof BlockHitResult bhr) {
                    BlockPos pos = bhr.getBlockPos();
                    BlockState state = p.level().getBlockState(pos);
                    SoundType soundType = state.getSoundType();
                    
                    float pitch = 1.0F;
                    float volume = soundType.getVolume() - (0.75f * p.getRandom().nextFloat());

                    p.level().playLocalSound(
                            pos.getX(), pos.getY(), pos.getZ(),
                            ModSoundEvents.DRILL_DIG.get(),
                            SoundSource.BLOCKS,
                            volume,
                            pitch,
                            false
                    );
                    
                    for (int i = 0; i < 5; i++) {
                        Minecraft.getInstance().particleEngine
                                .addBlockHitEffects(pos, bhr);
                    }
                }
            } else if ("inhabitants:drill_start_sound".equals(id)) {

                p.level().playLocalSound(
                        p.getX(), p.getY(), p.getZ(),
                        ModSoundEvents.DRILL_START.get(), SoundSource.PLAYERS,
                        0.25f, 1.0f, false);
                
            } else if ("inhabitants:drill_loop_sound".equals(id)) {
                if (ModTickableSounds.DrillLoop.currentSound == null ||
                        ModTickableSounds.DrillLoop.currentSound.isStopped()) {
                    
                    ModTickableSounds.DrillLoop.currentSound = new ModTickableSounds.DrillLoop(p);
                    Minecraft.getInstance().getSoundManager()
                            .play(ModTickableSounds.DrillLoop.currentSound);
                }
            } else if ("inhabitants:drill_stop_sound".equals(id)) {
                if (ModTickableSounds.DrillLoop.currentSound != null) {

                    ModTickableSounds.DrillLoop.currentSound.stopLoop();
                    ModTickableSounds.DrillLoop.currentSound = null;
                }

                p.level().playLocalSound(
                        p.getX(), p.getY(), p.getZ(),
                        ModSoundEvents.DRILL_STOPPED.get(), SoundSource.PLAYERS,
                        0.25f, 1.0f, false);
            }
        });
    }
}
