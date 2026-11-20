package com.lineage.client;

import com.lineage.ability.Ability;
import com.lineage.data.LineageData;
import com.lineage.origin.Origin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

/** Ability bar: one slot per active ability, with keybind label and a cooldown sweep. */
public final class AbilityHud {
    public static final IGuiOverlay OVERLAY = (gui, g, partialTick, w, h) -> render(g, w, h);

    private AbilityHud() {}

    private static void render(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        Origin origin = ClientState.origin();
        if (origin == null) return;

        LineageData data = ClientState.DATA;
        List<Ability> actives = origin.activeAbilitiesFor(data);
        int n = Math.min(actives.size(), KeyBinds.ABILITY.length);
        if (n == 0) return;

        Font font = mc.font;
        int size = 22, gap = 4;
        int total = n * (size + gap) - gap;
        int x0 = Math.min(w / 2 + 98, w - total - 6);
        int y0 = h - size - 4;
        long now = mc.level.getGameTime();

        // Evolution progress bar above the slots.
        int barY = y0 - 15;
        g.fill(x0, barY, x0 + total, barY + 3, 0xAA000000);
        g.fill(x0, barY, x0 + (int) (total * (data.getXp() / (float) origin.xpPerPoint())), barY + 3, 0xFF55AAFF);
        if (data.getPoints() > 0) {
            String s = "\u2605 " + data.getPoints() + " [" + KeyBinds.EVOLUTIONS.getTranslatedKeyMessage().getString() + "]";
            g.drawString(font, s, x0, barY - 10, 0xFFFFD700, true);
        }

        for (int i = 0; i < n; i++) {
            Ability a = actives.get(i);
            int x = x0 + i * (size + gap);
            long remain = data.cooldownEnd(a.id) - now;
            boolean cooling = remain > 0 && a.cooldownTicks > 0;

            UiUtil.panel(g, x, y0, size, size, 0xAA000000, cooling ? 0xFF555555 : 0xFFFFD700);
            g.renderItem(a.iconStack(), x + 3, y0 + 3);
            g.drawString(font, KeyBinds.ABILITY[i].getTranslatedKeyMessage(), x + 2, y0 - 9, 0xFFFFFFFF, true);

            if (cooling) {
                // Items render at a raised z-level, so lift our overlay above them.
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                float frac = Math.min(1f, remain / (float) a.cooldownTicks);
                g.fill(x + 1, y0 + 1, x + size - 1, y0 + 1 + (int) ((size - 2) * frac), 0xCC000000);
                String secs = String.valueOf((int) Math.ceil(remain / 20.0));
                g.drawString(font, secs, x + (size - font.width(secs)) / 2, y0 + 7, 0xFFFFFFFF, true);
                g.pose().popPose();
            }
        }
    }
}
