package com.jeremyseq.inhabitants.items.javelin;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;

import net.minecraftforge.api.distmarker.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class JavelinClient {
    public static void initializeJavelinClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private JavelinItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new JavelinItemRenderer();

                return this.renderer;
            }
        });
    }
}
