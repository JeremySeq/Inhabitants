package com.jeremyseq.inhabitants.items.armor;

import com.jeremyseq.inhabitants.items.GazerPodItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class GazerPodArmorRenderer extends GeoArmorRenderer<GazerPodItem> {
    public GazerPodArmorRenderer() {
        super(new GazerPodArmorModel());
    }
}
