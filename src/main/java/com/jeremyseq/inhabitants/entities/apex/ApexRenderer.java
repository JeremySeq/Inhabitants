package com.jeremyseq.inhabitants.entities.apex;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ApexRenderer extends GeoEntityRenderer<ApexEntity> {
    public ApexRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ApexModel());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ApexEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/apex.png");
    }
}
