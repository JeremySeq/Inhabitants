package com.jeremyseq.inhabitants.items.events;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID)
public class InteractionEvents {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Level level = player.level();
        InteractionHand hand = event.getHand();
        ItemStack stack = player.getItemInHand(hand);

        if (stack.getItem() == ModItems.GIANT_BONE.get() && event.getTarget() instanceof Wolf wolf) {
            if (!level.isClientSide && wolf.isTame() && wolf.getOwnerUUID() != null && wolf.getOwnerUUID().equals(player.getUUID())) {
                wolf.setHealth(wolf.getMaxHealth());
                wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2400, 1, true, true, true));

                stack.shrink(1);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }
}
