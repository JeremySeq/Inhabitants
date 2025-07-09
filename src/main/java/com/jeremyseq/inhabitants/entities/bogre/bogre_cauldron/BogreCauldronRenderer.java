package com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BogreCauldronRenderer extends GeoEntityRenderer<BogreCauldronEntity> {
    public BogreCauldronRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BogreCauldronModel());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull BogreCauldronEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/bogre_cauldron.png");
    }
}
