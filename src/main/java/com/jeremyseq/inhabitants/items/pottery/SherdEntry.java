package com.jeremyseq.inhabitants.items.pottery;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.util.HashMap;
import java.util.Map;

public class SherdEntry implements ItemLike {

    public static final Map<String, SherdEntry> SHERDS = new HashMap<>();

    public final String type;
    public final ResourceKey sherdPattern;
    public final ResourceLocation itemId;
    public final Lazy<CustomSherdItem> item;

    public SherdEntry(String type) {

        this.type = type;
        this.sherdPattern = ResourceKey.create(Registries.DECORATED_POT_PATTERNS, ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, type + "_pottery_pattern"));
        this.itemId = ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, type + "_pottery_sherd");
        this.item = new Lazy(() -> new CustomSherdItem(this));
    }

    public static SherdEntry of(String type) {

        SherdEntry entry = new SherdEntry(type);
        SHERDS.put(type, entry);
        return entry;
    }

    @Override
    public Item asItem() {

        return this.item.get();
    }
}
