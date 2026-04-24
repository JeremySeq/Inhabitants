package com.jeremyseq.inhabitants.entities.bulltoad;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BulltoadModel extends GeoModel<BulltoadEntity> {
    @Override
    public ResourceLocation getModelResource(BulltoadEntity animatable) {
        if (animatable.getBreedStage() == -1) {
            return ResourceLocation.fromNamespaceAndPath("inhabitants", "geo/baby_bulltoad.geo.json");
        }
        return ResourceLocation.fromNamespaceAndPath("inhabitants", "geo/bulltoad.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BulltoadEntity animatable) {
        if (animatable.getBreedStage() == -1) {
            return ResourceLocation.fromNamespaceAndPath("inhabitants", "textures/entity/bulltoad/baby_bulltoad.png");
        } else if (animatable.getBreedStage() == 1) {
            return ResourceLocation.fromNamespaceAndPath("inhabitants", "textures/entity/bulltoad/withtoads_0.png");
        } else if (animatable.getBreedStage() == 2) {
            return ResourceLocation.fromNamespaceAndPath("inhabitants", "textures/entity/bulltoad/withtoads_1.png");
        }
        return ResourceLocation.fromNamespaceAndPath("inhabitants", "textures/entity/bulltoad/bulltoad.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BulltoadEntity animatable) {
        if (animatable.getBreedStage() == -1) {
            return ResourceLocation.fromNamespaceAndPath("inhabitants", "animations/baby_bulltoad.animation.json");
        }
        return ResourceLocation.fromNamespaceAndPath("inhabitants", "animations/bulltoad.animation.json");
    }
}
