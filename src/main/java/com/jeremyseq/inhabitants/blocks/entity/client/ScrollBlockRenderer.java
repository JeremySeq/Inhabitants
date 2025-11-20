package com.jeremyseq.inhabitants.blocks.entity.client;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.blocks.ScrollBlock;
import com.jeremyseq.inhabitants.blocks.entity.ScrollBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class ScrollBlockRenderer extends GeoBlockRenderer<ScrollBlockEntity> {
    public ScrollBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new ScrollBlockModel());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ScrollBlockEntity animatable) {
        return switch (animatable.getBlockState().getValue(ScrollBlock.SIZE)) {
            case 0 -> ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/block/scroll_small.png");
            case 2 -> ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/block/scroll_large.png");
            default -> ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/block/scroll_medium.png");
        };
    }
}
