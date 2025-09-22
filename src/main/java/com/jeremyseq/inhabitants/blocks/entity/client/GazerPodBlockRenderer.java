package com.jeremyseq.inhabitants.blocks.entity.client;

import com.jeremyseq.inhabitants.blocks.entity.GazerPodBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class GazerPodBlockRenderer extends GeoBlockRenderer<GazerPodBlockEntity> {
    public GazerPodBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new GazerPodBlockModel());
    }
}
