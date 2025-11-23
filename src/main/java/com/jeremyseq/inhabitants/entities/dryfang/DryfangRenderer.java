package com.jeremyseq.inhabitants.entities.dryfang;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DryfangRenderer extends GeoEntityRenderer<DryfangEntity> {
    private static final ResourceLocation NORMAL_TEXTURE = ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/dryfang.png");

    private static final ResourceLocation ANGRY_TEXTURE = ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/entity/dryfang_angry.png");


    public DryfangRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DryfangModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull DryfangEntity animatable) {
        if (animatable.isAngry()) {
            return ANGRY_TEXTURE;
        } else {
            return NORMAL_TEXTURE;
        }
    }
}
