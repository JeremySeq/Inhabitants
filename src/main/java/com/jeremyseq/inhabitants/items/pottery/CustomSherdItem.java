package com.jeremyseq.inhabitants.items.pottery;

import net.minecraft.world.item.Item;

public class CustomSherdItem extends Item {

    public final SherdEntry entry;

    public CustomSherdItem(SherdEntry entry) {

        super(new Item.Properties());
        this.entry = entry;
    }
}