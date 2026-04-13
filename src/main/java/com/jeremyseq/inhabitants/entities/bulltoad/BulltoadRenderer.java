package com.jeremyseq.inhabitants.entities.bulltoad;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BulltoadRenderer extends GeoEntityRenderer<BulltoadEntity> {
    public BulltoadRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BulltoadModel());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull BulltoadEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/bulltoad/bulltoad.png");
    }
}
