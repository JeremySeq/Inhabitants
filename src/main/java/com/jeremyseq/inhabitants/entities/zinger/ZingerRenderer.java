package com.jeremyseq.inhabitants.entities.zinger;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ZingerRenderer extends GeoEntityRenderer<ZingerEntity> {
    public ZingerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ZingerModel());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ZingerEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/zinger.png");
    }
}
