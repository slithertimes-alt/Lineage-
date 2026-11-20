package com.lineage.client;

import net.minecraft.client.gui.GuiGraphics;

public final class UiUtil {
    private UiUtil() {}

    /** Filled rectangle with a 1px border. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }
}
