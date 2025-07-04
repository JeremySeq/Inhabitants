package com.jeremyseq.inhabitants.entities.warped_clam;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class WarpedClamRenderer extends GeoEntityRenderer<WarpedClamEntity> {
    public WarpedClamRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new WarpedClamModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull WarpedClamEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/warped_clam.png");
    }
}
