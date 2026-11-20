package com.lineage.client.screen;

import com.lineage.ability.Ability;
import com.lineage.client.ClientState;
import com.lineage.client.UiUtil;
import com.lineage.network.Network;
import com.lineage.network.Packets;
import com.lineage.origin.Origin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Origin picker: list on the left, details in the middle (scrollable), live player preview on the right. */
public class OriginSelectScreen extends Screen {
    private static final int LIST_W = 120;

    private final List<Origin> origins;
    private int selected = 0;
    private double scroll = 0;
    private int contentHeight = 0;
    private int px, py, pw, ph;

    public OriginSelectScreen() {
        super(Component.literal("Choose Your Origin"));
        origins = new ArrayList<>(ClientState.origins.values());
        origins.sort(Comparator.comparing(Origin::name));
    }

    @Override
    protected void init() {
        pw = Math.min(width - 20, 480);
        ph = Math.min(height - 20, 280);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        addRenderableWidget(Button.builder(Component.literal("Choose Origin"), b -> choose())
                .bounds(px + pw - 118, py + ph - 28, 110, 20).build());
    }

    private void choose() {
        if (origins.isEmpty()) return;
        ClientState.awaitingServer = true;
        Network.CHANNEL.sendToServer(new Packets.ChooseOrigin(origins.get(selected).id()));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        UiUtil.panel(g, px, py, pw, ph, 0xE0101018, 0xFF3A3A5A);
        g.drawCenteredString(font, title, px + pw / 2, py + 7, 0xFFFFD700);

        // ---- origin list ----
        int lx = px + 8, ly = py + 22;
        for (int i = 0; i < origins.size(); i++) {
            Origin o = origins.get(i);
            int y = ly + i * 26;
            if (y + 24 > py + ph - 8) break;
            boolean sel = i == selected;
            boolean hover = mx >= lx && mx < lx + LIST_W && my >= y && my < y + 24;
            UiUtil.panel(g, lx, y, LIST_W, 24, sel ? 0xFF2A2A4A : hover ? 0xFF222236 : 0xFF14141E,
                    sel ? 0xFFFFD700 : 0xFF333344);
            g.renderItem(o.iconStack(), lx + 4, y + 4);
            g.drawString(font, o.name(), lx + 26, y + 8, 0xFFFFFFFF, false);
        }
        if (origins.isEmpty()) {
            super.render(g, mx, my, pt);
            return;
        }

        // ---- details (scrollable) ----
        Origin o = origins.get(selected);
        int dx = px + 136, dw = pw - 136 - 126, top = py + 22, bottom = py + ph - 8;
        g.enableScissor(dx, top, dx + dw, bottom);
        int startY = top - (int) scroll;
        int y = startY;

        g.pose().pushPose();
        g.pose().translate(dx, y, 0);
        g.pose().scale(1.5f, 1.5f, 1f);
        g.drawString(font, o.name(), 0, 0, 0xFFFFD700, true);
        g.pose().popPose();
        y += 18;

        int impactColor = o.impact() == 1 ? 0xFF55FF55 : o.impact() == 2 ? 0xFFFFAA00 : 0xFFFF5555;
        String pips = "\u25cf".repeat(o.impact()) + "\u25cb".repeat(3 - o.impact());
        g.drawString(font, "Impact: " + pips, dx, y, impactColor, false);
        y += 14;

        for (FormattedCharSequence line : font.split(Component.literal(o.description()), dw)) {
            g.drawString(font, line, dx, y, 0xFFBBBBBB, false);
            y += 10;
        }
        y += 8;

        g.drawString(font, "Abilities", dx, y, 0xFFFFFFFF, true);
        y += 12;
        for (Ability a : o.abilities()) {
            g.renderItem(a.iconStack(), dx, y);
            g.drawString(font, a.name + (a.isActive() ? " [Active]" : ""), dx + 20, y + 1,
                    a.isActive() ? 0xFF55FFFF : 0xFFFFD700, false);
            int ty = y + 11;
            for (FormattedCharSequence line : font.split(Component.literal(a.description), dw - 20)) {
                g.drawString(font, line, dx + 20, ty, 0xFF999999, false);
                ty += 10;
            }
            y = Math.max(y + 18, ty + 4);
        }
        y += 4;
        g.drawString(font, "Evolutions: " + o.nodes().size() + " upgrades with branching paths", dx, y, 0xFF55AAFF, false);
        y += 12;
        contentHeight = y - startY;
        g.disableScissor();

        // ---- live preview ----
        if (minecraft != null && minecraft.player != null) {
            int cx = px + pw - 63;
            int feetY = py + ph - 42;
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, cx, feetY, 50,
                    (float) (cx - mx), (float) (feetY - 80 - my), minecraft.player);
        }

        super.render(g, mx, my, pt); // widgets (confirm button)
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int lx = px + 8, ly = py + 22;
        for (int i = 0; i < origins.size(); i++) {
            int y = ly + i * 26;
            if (mx >= lx && mx < lx + LIST_W && my >= y && my < y + 24) {
                selected = i;
                scroll = 0;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int viewH = ph - 30;
        scroll = Mth.clamp(scroll - delta * 12, 0, Math.max(0, contentHeight - viewH));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (!origins.isEmpty()) {
            if (key == GLFW.GLFW_KEY_DOWN) { selected = (selected + 1) % origins.size(); scroll = 0; return true; }
            if (key == GLFW.GLFW_KEY_UP) { selected = (selected - 1 + origins.size()) % origins.size(); scroll = 0; return true; }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { choose(); return true; }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
}
