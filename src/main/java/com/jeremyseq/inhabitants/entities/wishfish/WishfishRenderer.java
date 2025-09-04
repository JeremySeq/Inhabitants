package com.jeremyseq.inhabitants.entities.wishfish;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class WishfishRenderer extends GeoEntityRenderer<WishfishEntity> {
    public WishfishRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new WishfishModel());
        this.shadowRadius = 0.3f;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull WishfishEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/wishfish.png");
    }
}
