package com.jeremyseq.inhabitants.entities.concher;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ConcherRenderer extends GeoEntityRenderer<ConcherEntity> {
    public ConcherRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ConcherModel());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ConcherEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, String.format("textures/entity/concher_%d.png", animatable.getStage()));}
}
