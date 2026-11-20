package com.lineage.client.screen;

import com.lineage.ability.Ability;
import com.lineage.client.ClientState;
import com.lineage.client.KeyBinds;
import com.lineage.client.UiUtil;
import com.lineage.data.LineageData;
import com.lineage.network.Network;
import com.lineage.network.Packets;
import com.lineage.origin.Evolution;
import com.lineage.origin.EvolutionNode;
import com.lineage.origin.Origin;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/** Skill-tree style evolution screen. Auto-lays out nodes by depth; drag to pan; click a gold node to evolve. */
public class EvolutionScreen extends Screen {
    private static final int BOX = 28, GX = 70, GY = 40;

    private static final int C_UNLOCKED = 0xFF55FF55;
    private static final int C_AVAILABLE = 0xFFFFD700;
    private static final int C_NEED_POINTS = 0xFF997700;
    private static final int C_LOCKED = 0xFF555566;
    private static final int C_BLOCKED = 0xFFAA3333;

    private final Origin origin;
    private final Map<String, double[]> pos = new HashMap<>();
    private int maxDepth = 1;
    private double panX, panY;
    private EvolutionNode hovered;

    public EvolutionScreen() {
        super(Component.literal("Evolutions"));
        this.origin = ClientState.origin();
        layout();
    }

    private void layout() {
        if (origin == null) return;
        Map<String, Integer> depths = new HashMap<>();
        for (EvolutionNode n : origin.nodes()) depth(n, depths);
        Map<Integer, List<EvolutionNode>> byDepth = new TreeMap<>();
        for (EvolutionNode n : origin.nodes()) byDepth.computeIfAbsent(depths.get(n.id()), k -> new ArrayList<>()).add(n);
        for (Map.Entry<Integer, List<EvolutionNode>> e : byDepth.entrySet()) {
            List<EvolutionNode> col = e.getValue();
            for (int i = 0; i < col.size(); i++) {
                pos.put(col.get(i).id(), new double[]{e.getKey() * GX, (i - (col.size() - 1) / 2.0) * GY});
            }
            maxDepth = Math.max(maxDepth, e.getKey());
        }
    }

    private int depth(EvolutionNode n, Map<String, Integer> memo) {
        Integer cached = memo.get(n.id());
        if (cached != null) return cached;
        memo.put(n.id(), 1); // cycle guard
        int d = 1;
        for (String r : n.requires()) {
            EvolutionNode parent = origin.node(r);
            if (parent != null) d = Math.max(d, depth(parent, memo) + 1);
        }
        memo.put(n.id(), d);
        return d;
    }

    private int borderFor(EvolutionNode n, LineageData d) {
        if (d.hasNode(n.id())) return C_UNLOCKED;
        if (Evolution.isBlocked(origin, n, d)) return C_BLOCKED;
        if (!Evolution.requirementsMet(n, d)) return C_LOCKED;
        return d.getPoints() >= n.cost() ? C_AVAILABLE : C_NEED_POINTS;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        if (origin == null) {
            g.drawCenteredString(font, "You have no origin yet.", width / 2, height / 2, 0xFFFFFFFF);
            super.render(g, mx, my, pt);
            return;
        }
        LineageData d = ClientState.DATA;
        int baseX = (int) (width / 2.0 - maxDepth * GX / 2.0 + panX);
        int baseY = (int) (height / 2.0 + 12 + panY);
        hovered = null;

        // ---- connections ----
        for (EvolutionNode n : origin.nodes()) {
            double[] p = pos.get(n.id());
            int cx = baseX + (int) p[0], cy = baseY + (int) p[1];
            if (n.requires().isEmpty()) {
                connect(g, baseX, baseY, cx, cy, d.hasNode(n.id()));
            } else {
                for (String r : n.requires()) {
                    double[] pp = pos.get(r);
                    connect(g, baseX + (int) pp[0], baseY + (int) pp[1], cx, cy, d.hasNode(r) && d.hasNode(n.id()));
                }
            }
        }

        // ---- root (the origin itself) ----
        drawBox(g, baseX, baseY, origin.iconStack(), 0xFF55AAFF, false);

        // ---- nodes ----
        for (EvolutionNode n : origin.nodes()) {
            double[] p = pos.get(n.id());
            int cx = baseX + (int) p[0], cy = baseY + (int) p[1];
            boolean over = mx >= cx - BOX / 2 && mx < cx + BOX / 2 && my >= cy - BOX / 2 && my < cy + BOX / 2;
            if (over) hovered = n;
            int border = borderFor(n, d);
            drawBox(g, cx, cy, n.iconStack(), border, over);

            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            if (border == C_LOCKED || border == C_BLOCKED) {
                g.fill(cx - BOX / 2 + 1, cy - BOX / 2 + 1, cx + BOX / 2 - 1, cy + BOX / 2 - 1, 0x99000000);
            }
            if (!d.hasNode(n.id())) {
                String s = String.valueOf(n.cost());
                g.drawString(font, s, cx + BOX / 2 - font.width(s) - 2, cy + BOX / 2 - 9, 0xFFFFD700, true);
            }
            g.pose().popPose();
        }

        // ---- header ----
        g.drawCenteredString(font, origin.name() + " \u2014 Evolutions", width / 2, 10, 0xFFFFD700);
        g.drawCenteredString(font, "Evolution Points: " + d.getPoints(), width / 2, 24, 0xFFFFFFFF);
        int bw = 160, bx = width / 2 - bw / 2;
        g.fill(bx, 38, bx + bw, 42, 0xFF000000);
        g.fill(bx, 38, bx + (int) (bw * d.getXp() / (float) origin.xpPerPoint()), 42, 0xFF55AAFF);
        g.drawCenteredString(font, d.getXp() + " / " + origin.xpPerPoint() + " XP", width / 2, 46, 0xFF888899);
        g.drawCenteredString(font, "Click a gold node to evolve  \u2022  Drag to pan  \u2022  ["
                + KeyBinds.EVOLUTIONS.getTranslatedKeyMessage().getString() + "] to close",
                width / 2, height - 14, 0xFF777788);

        super.render(g, mx, my, pt);
        if (hovered != null) g.renderComponentTooltip(font, tooltip(hovered, d), mx, my);
    }

    private void drawBox(GuiGraphics g, int cx, int cy, ItemStack icon, int border, boolean hover) {
        UiUtil.panel(g, cx - BOX / 2, cy - BOX / 2, BOX, BOX, hover ? 0xFF2E2E48 : 0xFF181826, border);
        g.renderItem(icon, cx - 8, cy - 8);
    }

    private void connect(GuiGraphics g, int x1, int y1, int x2, int y2, boolean active) {
        int color = active ? C_UNLOCKED : C_LOCKED;
        int sx = x1 + BOX / 2, ex = x2 - BOX / 2, mid = (sx + ex) / 2;
        g.hLine(sx, mid, y1, color);
        g.vLine(mid, Math.min(y1, y2), Math.max(y1, y2), color);
        g.hLine(mid, ex, y2, color);
    }

    private List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        for (FormattedText ft : font.getSplitter().splitLines(text, width, Style.EMPTY)) out.add(ft.getString());
        return out;
    }

    private List<Component> tooltip(EvolutionNode n, LineageData d) {
        List<Component> t = new ArrayList<>();
        t.add(Component.literal(n.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (String line : wrap(n.description(), 200)) t.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        if (!n.abilities().isEmpty()) {
            t.add(Component.empty());
            for (Ability a : n.abilities()) {
                t.add(Component.literal("\u2022 " + a.name + (a.isActive() ? " [Active]" : " [Passive]"))
                        .withStyle(ChatFormatting.AQUA));
            }
        }
        t.add(Component.empty());
        if (d.hasNode(n.id())) {
            t.add(Component.literal("Unlocked").withStyle(ChatFormatting.GREEN));
        } else if (Evolution.isBlocked(origin, n, d)) {
            t.add(Component.literal("Blocked: you chose a conflicting path").withStyle(ChatFormatting.RED));
        } else if (!Evolution.requirementsMet(n, d)) {
            List<String> missing = new ArrayList<>();
            for (String r : n.requires()) if (!d.hasNode(r)) missing.add(origin.node(r).name());
            t.add(Component.literal("Requires: " + String.join(", ", missing)).withStyle(ChatFormatting.RED));
        } else if (d.getPoints() < n.cost()) {
            t.add(Component.literal("Needs " + n.cost() + " point(s) - you have " + d.getPoints()).withStyle(ChatFormatting.YELLOW));
        } else {
            t.add(Component.literal("Click to evolve (cost " + n.cost() + ")").withStyle(ChatFormatting.GREEN));
        }
        if (!n.excludes().isEmpty() && !d.hasNode(n.id())) {
            List<String> names = new ArrayList<>();
            for (String x : n.excludes()) names.add(origin.node(x).name());
            t.add(Component.literal("Excludes: " + String.join(", ", names)).withStyle(ChatFormatting.DARK_RED));
        }
        return t;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && hovered != null && origin != null && Evolution.canUnlock(origin, hovered, ClientState.DATA)) {
            Network.CHANNEL.sendToServer(new Packets.UnlockNode(hovered.id()));
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 1 || button == 2 || (button == 0 && hovered == null)) {
            panX += dx;
            panY += dy;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (KeyBinds.EVOLUTIONS.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override public boolean isPauseScreen() { return false; }
}
