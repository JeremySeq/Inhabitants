package com.jeremyseq.inhabitants.entities.dryfang;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class EatMeatDropGoal extends Goal {
    private static final TagKey<Item> MEAT_ITEMS =
        TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "dryfang_meat")
        );

    private final DryfangEntity dryfang;
    private final double speed;
    private final double range;
    private final double eatDistanceSq = 2.0D * 2.0D;
    private ItemEntity targetItem;
    private int eatingWaitTicks;


    public EatMeatDropGoal(DryfangEntity dryfang, double speed, double range) {
        this.dryfang = dryfang;
        this.speed = speed;
        this.range = range;
        this.eatingWaitTicks = 0;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!dryfang.isAlive()) return false;

        // get da meat
        Level level = dryfang.level();
        List<ItemEntity> items = level.getEntitiesOfClass(
                ItemEntity.class,
                dryfang.getBoundingBox().inflate(range),
                item -> item.isAlive() && item.getItem().is(MEAT_ITEMS)
        );

        // cant use if theres no meat
        if (items.isEmpty()) {
            targetItem = null;
            return false;
        }

        // pick nearest meat drop
        items.sort(Comparator.comparingDouble(dryfang::distanceToSqr));
        targetItem = items.get(0);
        return targetItem != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (targetItem == null) return false;
        if (!dryfang.isAlive()) return false;
        if (!targetItem.isAlive()) return false;
        // stop if out of range
        return dryfang.distanceToSqr(targetItem) <= range * range;
    }

    @Override
    public void start() {
        // ensure navigation targets the item immediately
        if (targetItem != null) {
            dryfang.getNavigation().moveTo(targetItem, speed);
            dryfang.playSound(SoundEvents.WOLF_GROWL, 1.0F, 1.0F);
        }
    }

    @Override
    public void stop() {
        targetItem = null;
        dryfang.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetItem == null) return;

        // look at and move towards the item
        dryfang.getLookControl().setLookAt(targetItem, 30.0F, 30.0F);
        dryfang.getNavigation().moveTo(targetItem, speed);

        // if close enough, start eating
        double distSq = dryfang.distanceToSqr(targetItem);
        if (distSq <= eatDistanceSq) {
            eatingWaitTicks++;
        }

        // CONSUME (after 1 sec)
        if (eatingWaitTicks >= 20) {
            targetItem.discard();
            dryfang.heal(4.0F);
            dryfang.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
            eatingWaitTicks = 0;
            targetItem = null;
        }
    }
}
