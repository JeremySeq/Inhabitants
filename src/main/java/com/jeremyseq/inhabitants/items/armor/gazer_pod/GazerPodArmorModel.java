package com.jeremyseq.inhabitants.items.armor.gazer_pod;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.items.GazerPodItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GazerPodArmorModel extends GeoModel<GazerPodItem> {
    @Override
    public ResourceLocation getModelResource(GazerPodItem gazerPodItem) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "geo/gazer_pod_item.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GazerPodItem gazerPodItem) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/armor/gazer_pod_helmet.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GazerPodItem gazerPodItem) {
        return null;
    }
}
