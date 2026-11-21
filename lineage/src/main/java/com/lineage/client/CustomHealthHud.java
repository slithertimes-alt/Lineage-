package com.lineage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.Locale;

/** Compact health bar with numerical HP, independent of the player's max-health value. */
public final class CustomHealthHud {
    public static final IGuiOverlay OVERLAY = (gui, g, partialTick, width, height) -> render(g, width, height);

    private CustomHealthHud() {}

    private static void render(GuiGraphics g, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isCreative() || mc.player.isSpectator()) return;

        float health = mc.player.getHealth();
        float maxHealth = Math.max(1f, mc.player.getMaxHealth());
        float fraction = Math.max(0f, Math.min(1f, health / maxHealth));
        int x = 8;
        int y = height - 49;
        int panelWidth = Math.min(158, Math.max(132, width / 5));
        UiUtil.panel(g, x, y, panelWidth, 34, 0xD9101721, 0xFF42576B);

        String label = String.format(Locale.ROOT, "HP %.1f / %.1f", health, maxHealth);
        g.drawString(mc.font, label, x + 8, y + 5, 0xFFF0F5FC, true);

        int barX = x + 8;
        int barY = y + 21;
        int barWidth = panelWidth - 16;
        g.fill(barX, barY, barX + barWidth, barY + 6, 0xFF070B10);
        int fill = Math.round(barWidth * fraction);
        if (fill > 0) {
            int color = fraction > 0.55f ? 0xFF65D995 : fraction > 0.25f ? 0xFFECC75A : 0xFFE85B62;
            g.fill(barX + 1, barY + 1, barX + Math.max(1, fill - 1), barY + 5, color);
        }

        float absorption = mc.player.getAbsorptionAmount();
        if (absorption > 0) {
            String shield = String.format(Locale.ROOT, "+%.1f", absorption);
            g.drawString(mc.font, shield, x + panelWidth - mc.font.width(shield) - 7, y + 5, 0xFFFFD95E, true);
        }
    }
}
