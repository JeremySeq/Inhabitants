package com.jeremyseq.inhabitants.entities.catcher;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CatcherRenderer extends GeoEntityRenderer<CatcherEntity> {
    public CatcherRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CatcherModel());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull CatcherEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/catcher.png");
    }
}
