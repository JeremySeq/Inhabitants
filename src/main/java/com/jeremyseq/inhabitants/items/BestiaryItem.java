package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
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
    private Entity lastTarget;
    private long timeBetweenTargets = 0;

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
            if (target != lastTarget) {
                timeBetweenTargets++;

                if (timeBetweenTargets > 10) {
                    BestiaryOverlay.showText(getBestiaryInfo(target));
                    lastTarget = target;
                    timeBetweenTargets = 0;
                }
            } else {
                timeBetweenTargets = 0; // Reset when looking at the same entity
            }
        } else {
            if (lastTarget != null) {
                timeBetweenTargets++;

                if (timeBetweenTargets > 10) {
                    BestiaryOverlay.hide();
                    lastTarget = null;
                    timeBetweenTargets = 0;
                }
            }
        }

        BestiaryOverlay.tick();
    }

    public static List<String> getBestiaryInfo(Entity entity) {
        List<String> info = new ArrayList<>();
        if (entity instanceof Zombie) {
            info.add("ITS GOING TO EAT US. AHHHHHHHHHHH!");
        }
        else if (entity instanceof Pig) {
            info.add("Mmmmmm. Bacon.");
        }
        else if (entity instanceof BogreEntity) {
            info.add("The Bogre is territorial and doesn’t welcome visitors. However, if you manage to cajole the Bogre, he might consider you a new friend! Just a tip — bring some raw fish; it could come in handy.");
            info.add("If you earn the trust of this mighty ogroid creature, he may even treat you to one of his soups. The taste? Not so great. But the saturation? Unmatched!");
            info.add("Despite his massive size, the Bogre is gentle when it comes to crafting. He’s surprisingly skilled at making bone weapons — just bring him some bones.");
            info.add("If, for some reason, you choose to fight him, be warned: the Bogre won’t go down easily. He’s a tough opponent and fiercely defends his swamp!");
        }
        return info;
    }
}
