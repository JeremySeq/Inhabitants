package com.jeremyseq.inhabitants.blocks.entity.client;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.blocks.entity.GazerPodBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class GazerPodBlockRenderer extends GeoBlockRenderer<GazerPodBlockEntity> {
    public GazerPodBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new GazerPodBlockModel());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull GazerPodBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/block/gazer_pod_block.png");
    }
}
