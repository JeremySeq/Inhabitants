package com.jeremyseq.inhabitants.blocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class ScrollItem extends BlockItem {
    private final String langKey;

    public ScrollItem(Block block, Properties props, String langKey) {
        super(block, props);
        this.langKey = langKey;
    }

    @Override
    public @NotNull String getDescriptionId() {
        return langKey;
    }
}

