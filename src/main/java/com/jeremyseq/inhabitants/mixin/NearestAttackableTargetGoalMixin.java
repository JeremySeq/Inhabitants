package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.effects.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(NearestAttackableTargetGoal.class)
public abstract class NearestAttackableTargetGoalMixin {

    @Shadow @Nullable
    protected LivingEntity target;

    @Inject(method = "findTarget", at = @At("TAIL"))
    private void inhabitants$ignorePlayersWithEffect(CallbackInfo ci) {
        if (this.target instanceof Player player) {

            var mob = ((TargetGoalAccessor) this).inhabitants$getMob();

            // undead only
            if (mob.getMobType() != MobType.UNDEAD) {
                return;
            }

            if (player.hasEffect(ModEffects.ROTTING_DISGUISE.get())) {
                this.target = null;
            }
        }
    }
}

