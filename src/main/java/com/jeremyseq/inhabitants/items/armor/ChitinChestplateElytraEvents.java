package com.jeremyseq.inhabitants.items.armor;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.items.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID, bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public class ChitinChestplateElytraEvents {
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer =
                    event.getSkin(skin);
            if (renderer != null) {
                // add new elytra layer
                renderer.addLayer(new ElytraLayer<>(renderer, Minecraft.getInstance().getEntityModels()) {
                    @Override
                    public boolean shouldRender(@NotNull ItemStack stack, @NotNull AbstractClientPlayer player) {
                        // only render if the chestplate is the chitin elytra chestplate
                        return stack.getItem() == ModItems.CHITIN_CHESTPLATE_ELYTRA.get();
                    }
                });
            }
        }
    }
}
