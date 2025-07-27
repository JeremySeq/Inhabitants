package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.overlays.BestiaryOverlay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class BestiaryItem extends Item {
    public BestiaryItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void onInventoryTick(ItemStack stack, Level level, Player player, int slotIndex, int selectedIndex) {
        super.onInventoryTick(stack, level, player, slotIndex, selectedIndex);
        if (slotIndex != selectedIndex) {
            BestiaryOverlay.hide();
            return;
        }

        // Raytrace logic
        double reach = 20;
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 reachVec = eyePos.add(lookVec.scale(reach));

        AABB aabb = player.getBoundingBox().expandTowards(lookVec.scale(reach)).inflate(1.0D);

        EntityHitResult result = ProjectileUtil.getEntityHitResult(
                player, eyePos, reachVec, aabb,
                e -> e instanceof LivingEntity && e.isPickable(),
                reach * reach
        );

        if (result != null && result.getEntity() instanceof LivingEntity target) {
            String name = target.getName().getString();
            float hp = target.getHealth();
            float maxHp = target.getMaxHealth();
            String type = target.getType().toShortString(); // or getRegistryName().toString()

            List<String> lines = new ArrayList<>();
//            lines.add(name);
//            lines.add("HP: " + (int) hp + " / " + (int) maxHp);
            lines.add(getBestiaryInfo(target));

            BestiaryOverlay.showText(lines);
        } else {
            BestiaryOverlay.hide();
        }

    }

    public static String getBestiaryInfo(Entity entity) {
        if (entity instanceof Zombie zombie) {
            return "ITS GOING TO EAT US AHHHHHHHHHHH";
        }
        else if (entity instanceof Pig) {
            return "Mmmmmm. Bacon.";
        }
        return "";
    }
}
