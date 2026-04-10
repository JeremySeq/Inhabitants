package com.jeremyseq.inhabitants.entities.nightmare;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class NightmareRenderer extends GeoEntityRenderer<NightmareEntity> {
    public NightmareRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new NightmareModel());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull NightmareEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/nightmare/nightmare_0.png");
    }
}
