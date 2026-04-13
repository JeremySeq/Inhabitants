package com.jeremyseq.inhabitants.mixin;

import com.jeremyseq.inhabitants.entities.goals.NightmarePanicGoal;
import net.minecraft.world.entity.PathfinderMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PathfinderMob.class)
public class NightmarePanicMixin {
    @Unique
    private boolean inhabitants$panicGoalAdded;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void inhabitants$addNightmarePanicGoal(CallbackInfo ci) {
        PathfinderMob mob = (PathfinderMob) (Object)this;

        if (this.inhabitants$panicGoalAdded) return;
        this.inhabitants$panicGoalAdded = true;

        mob.goalSelector.addGoal(0, new NightmarePanicGoal(mob,1.25D));
    }
}
