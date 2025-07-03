package com.jeremyseq.inhabitants.entities.abyssfish;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class AbyssfishRenderer extends GeoEntityRenderer<AbyssfishEntity> {
    public AbyssfishRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AbyssfishModel());
        this.shadowRadius = 0.3f;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull AbyssfishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/abyssfish.png");
    }
}
