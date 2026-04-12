package com.jeremyseq.inhabitants.gui.components;

import com.jeremyseq.inhabitants.Inhabitants;
import net.minecraft.resources.ResourceLocation;

public class ModGuiTextures {

    public static class SpriteRegion {
        public final ResourceLocation texture;
        public final int u, v;
        public final int width, height;
        public final int texWidth, texHeight;

        public SpriteRegion(ResourceLocation texture, int u, int v, int width, int height, int texWidth, int texHeight) {
            this.texture = texture;
            this.u = u;
            this.v = v;
            this.width = width;
            this.height = height;
            this.texWidth = texWidth;
            this.texHeight = texHeight;
        }
    }

    // --- Vanilla Textures ---
    public static final ResourceLocation RECIPE_BUTTON =
        ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/recipe_button.png");

    // --- Cauldron Screen ---
    public static final ResourceLocation CAULDRON_GUI = register("bogre_cauldron", "cauldron_gui");
    public static final ResourceLocation SLOT = register("bogre_cauldron", "slot");
    public static final ResourceLocation GHOST_SLOT = register("bogre_cauldron", "ghost_slot");
    public static final ResourceLocation BOWL_SLOT = register("bogre_cauldron", "bowl_slot");
    
    public static final ResourceLocation FIRE_SOURCE_ON = register("bogre_cauldron", "fire_source_on");
    public static final ResourceLocation FIRE_SOURCE_OFF = register("bogre_cauldron", "fire_source_off");
    public static final ResourceLocation ARROW = register("bogre_cauldron", "arrow");
    
    public static final ResourceLocation TIP_BUTTON = register("bogre_cauldron", "tip_button");
    public static final ResourceLocation TIP_BUTTON_HIGHLIGHT = register("bogre_cauldron", "tip_button_highlight");
    public static final ResourceLocation TIP_BUTTON_PRESSED = register("bogre_cauldron", "tip_button_pressed");
    
    public static final ResourceLocation[] BUBBLES = new ResourceLocation[] {
        register("bogre_cauldron", "bubbles_1"), register("bogre_cauldron", "bubbles_2"),
        register("bogre_cauldron", "bubbles_3"), register("bogre_cauldron", "bubbles_4"),
        register("bogre_cauldron", "bubbles_5"), register("bogre_cauldron", "bubbles_6"),
        register("bogre_cauldron", "bubbles_7"), register("bogre_cauldron", "bubbles_full")
    };
    
    // --- Cauldron Recipe Book ---
    public static final ResourceLocation RECIPE_BOOK_TEX = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/recipe_book.png");
    
    public static final SpriteRegion RECIPE_BOOK_WIDGET = new SpriteRegion(RECIPE_BOOK_TEX, 1, 1, 147, 166, 256, 256);
    public static final ResourceLocation TEXT_FIELD = register("bogre_cauldron/recipe_book", "text_field");
    public static final ResourceLocation TEXT_FIELD_HIGHLIGHTED = register("bogre_cauldron/recipe_book", "text_field_highlighted");
    
    public static final SpriteRegion SLOT_CRAFTABLE = new SpriteRegion(RECIPE_BOOK_TEX, 29, 206, 25, 25, 256, 256);
    public static final SpriteRegion SLOT_UNCRAFTABLE = new SpriteRegion(RECIPE_BOOK_TEX, 54, 206, 25, 25, 256, 256);
    
    public static final SpriteRegion FILTER_DISABLED = new SpriteRegion(RECIPE_BOOK_TEX, 152, 41, 26, 16, 256, 256);
    public static final SpriteRegion FILTER_DISABLED_HIGHLIGHTED = new SpriteRegion(RECIPE_BOOK_TEX, 152, 59, 26, 16, 256, 256);
    public static final SpriteRegion FILTER_ENABLED = new SpriteRegion(RECIPE_BOOK_TEX, 180, 41, 26, 16, 256, 256);
    public static final SpriteRegion FILTER_ENABLED_HIGHLIGHTED = new SpriteRegion(RECIPE_BOOK_TEX, 180, 59, 26, 16, 256, 256);
    
    public static final SpriteRegion PAGE_FORWARD = new SpriteRegion(RECIPE_BOOK_TEX, 1, 208, 11, 17, 256, 256);
    public static final SpriteRegion PAGE_FORWARD_HIGHLIGHTED = new SpriteRegion(RECIPE_BOOK_TEX, 1, 226, 11, 17, 256, 256);
    public static final SpriteRegion PAGE_BACKWARD = new SpriteRegion(RECIPE_BOOK_TEX, 15, 208, 11, 17, 256, 256);
    public static final SpriteRegion PAGE_BACKWARD_HIGHLIGHTED = new SpriteRegion(RECIPE_BOOK_TEX, 15, 226, 11, 17, 256, 256);

    private static ResourceLocation register(String dir, String fileName) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/gui/" + dir + "/" + fileName + ".png");
    }
    
    private static ResourceLocation register(String fileName) {
        return ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "textures/gui/" + fileName + ".png");
    }
}
