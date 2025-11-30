package com.jeremyseq.inhabitants.entities.dryfang;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

public class MeatyPlayerTargetGoal extends TargetGoal {
    private static final TagKey<Item> MEAT_ITEMS =
            TagKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "dryfang_meat"));

    private Player targetPlayer;

    public MeatyPlayerTargetGoal(Mob pMob, boolean pMustSee) {
        super(pMob, pMustSee);
        this.setFlags(EnumSet.of(Flag.TARGET));
    }


    private boolean hasMeat(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(MEAT_ITEMS)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canUse() {
        Level level = this.mob.level();

        // find player in normal follow range
        targetPlayer = level.getNearestPlayer(
                TargetingConditions.forCombat()
                        .range(this.getFollowDistance()),
                this.mob
        );

        // only start if they have meat
        return targetPlayer != null && hasMeat(targetPlayer);
    }

    @Override
    public void start() {
        // this is the step your version was missing
        this.mob.setTarget(targetPlayer);
        super.start();
    }

    @Override
    public boolean canContinueToUse() {
        // continue only while the target is valid and still has meat
        return targetPlayer != null
                && targetPlayer.isAlive()
                && hasMeat(targetPlayer)
                && !targetPlayer.isCreative()
                && this.mob.distanceTo(targetPlayer) <= this.getFollowDistance();
    }

    @Override
    public void stop() {
        // and clear target when done
        this.mob.setTarget(null);
        targetPlayer = null;
        super.stop();
    }
}
