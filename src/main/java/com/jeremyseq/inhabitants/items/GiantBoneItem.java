package com.jeremyseq.inhabitants.items;

import net.minecraft.world.item.*;

public class GiantBoneItem extends SwordItem {
    public GiantBoneItem() {
        super(Tiers.NETHERITE, 10, -3.5f, new Item.Properties().stacksTo(1).fireResistant());
    }


}
