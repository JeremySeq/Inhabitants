package com.jeremyseq.inhabitants.gui.components;

import net.minecraft.client.Minecraft;

/**
 * i like CSS
 * soo.. maybe i'll try to make this more like CSS
 * may be i'll be wrong, but i'm trying okay ?
 * sanks
 */
public class GuiLayout {
    public int x, y, width, height, zIndex;
    
    private final int parentX, parentY, parentW, parentH;
    private int marginLeft = 0, marginRight = 0, marginTop = 0, marginBottom = 0;

    public GuiLayout(int parentX, int parentY, int parentWidth, int parentHeight) {
        this.parentX = parentX;
        this.parentY = parentY;
        this.parentW = parentWidth;
        this.parentH = parentHeight;
        this.x = parentX;
        this.y = parentY;
        this.width = parentWidth;
        this.height = parentHeight;
        this.zIndex = 0;
    }

    public static GuiLayout screen(Minecraft minecraft) {
        return new GuiLayout(0, 0, minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight());
    }

    public GuiLayout createChild() {
        return new GuiLayout(this.x, this.y, this.width, this.height).zIndex(this.zIndex);
    }

    public GuiLayout width(int pixels) {
        this.width = pixels;
        return this;
    }

    public GuiLayout height(int pixels) {
        this.height = pixels;
        return this;
    }

    // can be used like "50%", "50"... thank me later
    public GuiLayout width(String stringValue) {
        this.width = parse(stringValue, parentW);
        return this;
    }

    public GuiLayout height(String stringValue) {
        this.height = parse(stringValue, parentH);
        return this;
    }

    public GuiLayout margin(int m) {
        this.marginTop = m;
        this.marginRight = m;
        this.marginBottom = m;
        this.marginLeft = m;
        return this;
    }

    public GuiLayout margin(int top, int right, int bottom, int left) {
        this.marginTop = top;
        this.marginRight = right;
        this.marginBottom = bottom;
        this.marginLeft = left;
        return this;
    }

    public GuiLayout padding(int p) {
        this.x += p;
        this.y += p;
        this.width -= p * 2;
        this.height -= p * 2;
        return this;
    }

    public GuiLayout align(String alignX, String alignY) {
        // x
        if (alignX != null) {
            switch (alignX.toLowerCase()) {
                case "center" -> this.x = parentX + (parentW - width) / 2;
                case "right" -> this.x = parentX + parentW - width - marginRight;
                default -> this.x = parentX + marginLeft; // left
            }
        }
        
        // y
        if (alignY != null) {
            switch (alignY.toLowerCase()) {
                case "center", "middle" -> this.y = parentY + (parentH - height) / 2;
                case "bottom" -> this.y = parentY + parentH - height - marginBottom;
                default -> this.y = parentY + marginTop; // top
            }
        }
        return this;
    }

    public GuiLayout center() {
        return align("center", "center");
    }

    public GuiLayout offset(int dx, int dy) {
        this.x += dx;
        this.y += dy;
        return this;
    }

    public GuiLayout clampToScreen(Minecraft minecraft) {
        int screenW = minecraft.getWindow().getGuiScaledWidth();
        int screenH = minecraft.getWindow().getGuiScaledHeight();
        
        if (this.x < 0) this.x = 0;
        if (this.y < 0) this.y = 0;
        if (this.x + this.width > screenW) this.x = screenW - this.width;
        if (this.y + this.height > screenH) this.y = screenH - this.height;
        return this;
    }

    public GuiLayout preserveAspect(float aspectRatio) {
        if (this.width > 0 && this.height > 0) {
            float currentRatio = (float) this.width / this.height;
            if (currentRatio > aspectRatio) {
                this.width = Math.round(this.height * aspectRatio);
            } else {
                this.height = Math.round(this.width / aspectRatio);
            }
        }
        return this;
    }
    
    // <_<, >_>, >_<
    public GuiLayout zIndex(int z) {
        this.zIndex = z;
        return this;
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width && 
               mouseY >= this.y && mouseY < this.y + this.height;
    }

    private int parse(String stringValue, int parentSize) {
        if (stringValue == null || stringValue.isEmpty()) return 0;
        
        String val = stringValue.trim().toLowerCase();
        try {
            if (val.endsWith("%")) {
                float percent = Float.parseFloat(val.replace("%", ""));
                return Math.round((float) parentSize * (percent / 100.0f));
            } else {
                return Integer.parseInt(val);
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
