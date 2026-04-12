package com.jeremyseq.inhabitants.events;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.items.ModItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID,
bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModModelEvents {

    private static final ResourceLocation CROSSBOW_SPIKE_LOADED =
        ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "item/crossbow_spike_loaded");

    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        event.register(CROSSBOW_SPIKE_LOADED);
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        
        ModelResourceLocation crossbow = new ModelResourceLocation("minecraft", "crossbow", "inventory");
        BakedModel originalModel = models.get(crossbow);
        
        if (originalModel != null) {
            BakedModel spikeModel = models.get(CROSSBOW_SPIKE_LOADED);
            if (spikeModel != null) {
                models.put(crossbow, new SpikeCrossbowModel(originalModel, spikeModel));
            }
        }
    }
    
    private static class SpikeCrossbowModel extends BakedModelWrapper<BakedModel> {
        private final BakedModel spikeModel;
        private final SpikeCrossbowOverrides customOverrides;

        public SpikeCrossbowModel(BakedModel original, BakedModel spikeModel) {
            super(original);
            this.spikeModel = spikeModel;
            this.customOverrides = new SpikeCrossbowOverrides(original.getOverrides());
        }

        @Override
        public @NotNull ItemOverrides getOverrides() {
            return customOverrides;
        }

        private class SpikeCrossbowOverrides extends ItemOverrides {
            private final ItemOverrides vanillaOverrides;

            public SpikeCrossbowOverrides(ItemOverrides vanilla) {
                this.vanillaOverrides = vanilla;
            }

            @Nullable
            @Override
            public BakedModel resolve(@NotNull BakedModel model, ItemStack stack,
                @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
                if (stack.is(Items.CROSSBOW) && CrossbowItem.isCharged(stack)) {
                    if (CrossbowItem.containsChargedProjectile(stack, ModItems.IMPALER_SPIKE.get())) {
                        return spikeModel;
                    }
                }
                
                // fallback to vanilla crossbow model
                return vanillaOverrides.resolve(model, stack, level, entity, seed);
            }
        }
    }
}
