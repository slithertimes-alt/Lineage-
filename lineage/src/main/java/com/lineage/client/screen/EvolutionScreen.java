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
import com.lineage.origin.EvolutionQuest;
import com.lineage.origin.Origin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Quest journal and evolution browser. */
public class EvolutionScreen extends Screen {
    private static final int GOLD = 0xFFFFCF68;
    private static final int AQUA = 0xFF62D8D0;
    private static final int GREEN = 0xFF66D99A;
    private static final int RED = 0xFFFF7777;
    private static final int MUTED = 0xFF95A4B8;
    private static final int PANEL = 0xF00D1420;
    private static final int TILE = 0xFF151F2D;
    private static final int ROW = 0xFF111A27;

    private final Origin origin;
    private String selectedId;
    private int listScroll;
    private int detailScroll;

    private int panelX, panelY, panelW, panelH;
    private int listX, listY, listW, listH;
    private int detailX, detailY, detailW, detailH;
    private int detailAreaTop, detailAreaBottom, claimX, claimY, claimW, claimH;
    private EvolutionNode hovered;

    public EvolutionScreen() {
        super(Component.literal("Evolution Journal"));
        this.origin = ClientState.origin();
        if (origin != null && !origin.nodes().isEmpty()) selectedId = origin.nodes().get(0).id();
    }

    @Override
    protected void init() {
        panelX = 12;
        panelY = 10;
        panelW = width - 24;
        panelH = height - 20;
        int bodyY = panelY + 58;
        int bodyH = Math.max(120, panelH - 116);
        listX = panelX + 12;
        listY = bodyY;
        listW = Math.max(125, Math.min((panelW - 36) * 38 / 100, (panelW - 40) * 45 / 100));
        listH = bodyH;
        detailX = listX + listW + 10;
        detailY = bodyY;
        detailW = panelX + panelW - 12 - detailX;
        detailH = bodyH;
        claimH = 25;
        claimX = detailX + 10;
        claimW = detailW - 20;
        claimY = panelY + panelH - 52;
        detailAreaTop = detailY + 78;
        detailAreaBottom = detailY + detailH - 5;
    }

    private EvolutionNode selected() {
        return origin == null || selectedId == null ? null : origin.node(selectedId);
    }

    private String status(EvolutionNode node, LineageData data) {
        if (data.hasNode(node.id())) return "UNLOCKED";
        if (Evolution.isBlocked(origin, node, data)) return "BRANCH LOCKED";
        if (!Evolution.prerequisitesMet(node, data)) return "PATH LOCKED";
        if (Evolution.questsComplete(node, data)) return "READY";
        return "IN PROGRESS";
    }

    private int statusColor(EvolutionNode node, LineageData data) {
        if (data.hasNode(node.id())) return GREEN;
        if (Evolution.isBlocked(origin, node, data)) return RED;
        if (!Evolution.prerequisitesMet(node, data)) return MUTED;
        if (Evolution.questsComplete(node, data)) return GOLD;
        return AQUA;
    }

    private int completedNodes(LineageData data) {
        int done = 0;
        if (origin != null) for (EvolutionNode node : origin.nodes()) if (data.hasNode(node.id())) done++;
        return done;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(0, 0, width, height, 0x9A050913);
        UiUtil.panel(g, panelX, panelY, panelW, panelH, PANEL, 0xFF3A5369);

        if (origin == null) {
            g.drawCenteredString(font, "Choose an origin to begin your evolution.", width / 2, height / 2 - 4, 0xFFFFFFFF);
            g.drawCenteredString(font, "Press Escape to return", width / 2, height / 2 + 14, MUTED);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        renderHeader(g);
        renderNodeList(g, mouseX, mouseY);
        renderDetails(g, mouseX, mouseY);
        renderFooter(g);
        super.render(g, mouseX, mouseY, partialTick);
        if (hovered != null) {
            g.renderTooltip(font, Component.literal(hovered.description()), mouseX, mouseY);
        }
    }

    private void renderHeader(GuiGraphics g) {
        LineageData data = ClientState.DATA;
        int iconY = panelY + 16;
        g.renderItem(origin.iconStack(), panelX + 16, iconY);
        g.drawString(font, origin.name(), panelX + 40, panelY + 13, GOLD, true);
        g.drawString(font, "EVOLUTION JOURNAL", panelX + 40, panelY + 27, MUTED, false);

        int done = completedNodes(data);
        String progress = done + " / " + origin.nodes().size() + " EVOLVED";
        int pw = font.width(progress);
        int chipX = panelX + panelW - pw - 34;
        UiUtil.panel(g, chipX, panelY + 14, pw + 22, 22, 0xFF142434, 0xFF31566A);
        g.drawString(font, progress, chipX + 11, panelY + 21, AQUA, true);
        g.fill(panelX + 12, panelY + 48, panelX + panelW - 12, panelY + 49, 0xFF2D3A4A);
    }

    private void renderNodeList(GuiGraphics g, int mouseX, int mouseY) {
        UiUtil.panel(g, listX, listY, listW, listH, 0xC90A111B, 0xFF25394D);
        g.drawString(font, "EVOLUTION PATH", listX + 10, listY + 8, 0xFFE6EDF5, true);
        g.drawString(font, "Select a node to inspect its quests", listX + 10, listY + 21, MUTED, false);
        int viewportTop = listY + 39;
        int viewportBottom = listY + listH - 8;
        hovered = null;
        g.enableScissor(listX + 3, viewportTop, listX + listW - 3, viewportBottom);
        LineageData data = ClientState.DATA;

        for (int i = 0; i < origin.nodes().size(); i++) {
            EvolutionNode node = origin.nodes().get(i);
            int cardY = viewportTop + i * 53 - listScroll;
            if (cardY + 47 < viewportTop || cardY > viewportBottom) continue;
            boolean selected = node.id().equals(selectedId);
            boolean over = mouseX >= listX + 6 && mouseX < listX + listW - 6
                    && mouseY >= cardY && mouseY < cardY + 47;
            int accent = statusColor(node, data);
            int fill = selected ? 0xFF1C2C3C : over ? 0xFF192637 : TILE;
            UiUtil.panel(g, listX + 6, cardY, listW - 12, 47, fill, selected ? accent : 0xFF253343);
            g.fill(listX + 7, cardY + 1, listX + 10, cardY + 46, accent);
            g.renderItem(node.iconStack(), listX + 16, cardY + 8);
            g.drawString(font, node.name(), listX + 39, cardY + 7, 0xFFF0F4F8, true);
            String label = status(node, data);
            g.drawString(font, label, listX + 39, cardY + 20, accent, false);
            int total = node.quests().size();
            int complete = Evolution.completedQuestCount(node, data);
            String count = total == 0 ? "No quests" : complete + "/" + total + " quests";
            g.drawString(font, count, listX + listW - 16 - font.width(count), cardY + 20, MUTED, false);
            int barX = listX + 39, barY = cardY + 35, barW = listW - 56;
            g.fill(barX, barY, barX + barW, barY + 3, 0xFF0A1017);
            if (total > 0) g.fill(barX, barY, barX + barW * complete / total, barY + 3, accent);
            if (over) hovered = node;
        }
        g.disableScissor();
    }

    private void renderDetails(GuiGraphics g, int mouseX, int mouseY) {
        UiUtil.panel(g, detailX, detailY, detailW, detailH, 0xC90A111B, 0xFF25394D);
        EvolutionNode node = selected();
        if (node == null) {
            g.drawCenteredString(font, "Choose an evolution on the left", detailX + detailW / 2, detailY + 28, MUTED);
            return;
        }

        LineageData data = ClientState.DATA;
        int accent = statusColor(node, data);
        g.renderItem(node.iconStack(), detailX + 12, detailY + 12);
        g.drawString(font, node.name(), detailX + 36, detailY + 11, GOLD, true);
        String label = status(node, data);
        int labelW = font.width(label);
        g.drawString(font, label, detailX + detailW - labelW - 12, detailY + 13, accent, true);

        int descY = detailY + 34;
        for (FormattedCharSequence line : font.split(Component.literal(node.description()), detailW - 24)) {
            if (descY > detailY + 61) break;
            g.drawString(font, line, detailX + 12, descY, 0xFFB7C4D2, false);
            descY += 11;
        }

        if (!node.requires().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (String id : node.requires()) {
                EvolutionNode required = origin.node(id);
                if (required != null) names.add(required.name());
            }
            if (!names.isEmpty()) g.drawString(font, "PATH: " + String.join("  ›  ", names),
                    detailX + 12, detailY + 68, Evolution.prerequisitesMet(node, data) ? GREEN : MUTED, false);
        }

        int titleY = detailAreaTop;
        g.drawString(font, "QUEST OBJECTIVES", detailX + 12, titleY, 0xFFE6EDF5, true);
        String questCount = Evolution.completedQuestCount(node, data) + " / " + node.quests().size() + " complete";
        g.drawString(font, questCount, detailX + detailW - 12 - font.width(questCount), titleY, MUTED, false);

        int rowsTop = titleY + 18;
        g.enableScissor(detailX + 5, rowsTop, detailX + detailW - 5, detailAreaBottom);
        int contentY = rowsTop + 4 - detailScroll;
        if (node.quests().isEmpty()) {
            g.drawString(font, "No quest requirements for this evolution.", detailX + 14, contentY + 9, MUTED, false);
            contentY += 28;
        }
        for (EvolutionQuest quest : node.quests()) {
            int rowY = contentY;
            boolean complete = Evolution.questProgress(node, quest, data) >= quest.amount;
            UiUtil.panel(g, detailX + 8, rowY, detailW - 16, 39, ROW, complete ? 0xFF285845 : 0xFF243141);
            String check = complete ? "✓" : "○";
            g.drawString(font, check, detailX + 16, rowY + 8, complete ? GREEN : MUTED, true);
            int textX = detailX + 34;
            int textMax = Math.max(40, detailW - 112);
            String description = truncate(quest.description, textMax);
            g.drawString(font, description, textX, rowY + 5, 0xFFE7EDF5, false);
            int value = Evolution.questProgress(node, quest, data);
            String amount = value + " / " + quest.amount;
            g.drawString(font, amount, detailX + detailW - 12 - font.width(amount), rowY + 5,
                    complete ? GREEN : MUTED, false);
            int barX = textX, barY = rowY + 24, barW = detailW - 55;
            g.fill(barX, barY, barX + barW, barY + 4, 0xFF081019);
            if (value > 0) g.fill(barX, barY, barX + barW * value / quest.amount, barY + 4,
                    complete ? GREEN : AQUA);
            contentY += 45;
        }

        contentY += 4;
        g.drawString(font, "REWARDS", detailX + 12, contentY, 0xFFE6EDF5, true);
        contentY += 17;
        if (node.abilities().isEmpty()) {
            g.drawString(font, "No new abilities", detailX + 14, contentY, MUTED, false);
            contentY += 18;
        } else {
            for (Ability ability : node.abilities()) {
                g.renderItem(ability.iconStack(), detailX + 12, contentY - 3);
                String kind = ability.isActive() ? "ACTIVE" : "PASSIVE";
                g.drawString(font, ability.name + "  ·  " + kind, detailX + 33, contentY, AQUA, false);
                contentY += 21;
            }
        }
        if (Evolution.isBlocked(origin, node, data)) {
            g.drawString(font, "This branch conflicts with an evolution you chose.", detailX + 12,
                    contentY + 3, RED, false);
        }
        g.disableScissor();

        boolean ready = Evolution.canUnlock(origin, node, data);
        int buttonFill = ready ? 0xFFB98638 : data.hasNode(node.id()) ? 0xFF1E4938 : 0xFF202B38;
        int buttonBorder = ready ? GOLD : data.hasNode(node.id()) ? GREEN : 0xFF354657;
        UiUtil.panel(g, claimX, claimY, claimW, claimH, buttonFill, buttonBorder);
        String buttonText = data.hasNode(node.id()) ? "EVOLUTION UNLOCKED"
                : ready ? "CLAIM EVOLUTION" : "COMPLETE QUESTS TO UNLOCK";
        g.drawCenteredString(font, buttonText, claimX + claimW / 2, claimY + 8,
                ready ? 0xFF111820 : data.hasNode(node.id()) ? GREEN : MUTED);
        if (ready && mouseX >= claimX && mouseX <= claimX + claimW
                && mouseY >= claimY && mouseY <= claimY + claimH) {
            g.renderTooltip(font, Component.literal("Claim " + node.name()), mouseX, mouseY);
        }
    }

    private void renderFooter(GuiGraphics g) {
        g.drawString(font, "Select a node to view its quests  •  Scroll to browse  •  ["
                + KeyBinds.EVOLUTIONS.getTranslatedKeyMessage().getString() + "] to close",
                panelX + 14, panelY + panelH - 16, MUTED, false);
    }

    private String truncate(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) return value;
        String suffix = "…";
        int end = value.length();
        while (end > 0 && font.width(value.substring(0, end) + suffix) > maxWidth) end--;
        return value.substring(0, end) + suffix;
    }

    private int maxListScroll() {
        int viewport = listH - 48;
        return Math.max(0, origin.nodes().size() * 53 - viewport);
    }

    private int detailContentHeight(EvolutionNode node) {
        return 24 + node.quests().size() * 45 + 25 + Math.max(1, node.abilities().size()) * 21;
    }

    private int maxDetailScroll(EvolutionNode node) {
        return Math.max(0, detailContentHeight(node) - (detailAreaBottom - detailAreaTop - 22));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || origin == null) return super.mouseClicked(mouseX, mouseY, button);
        EvolutionNode node = selected();
        if (mouseX >= claimX && mouseX <= claimX + claimW && mouseY >= claimY && mouseY <= claimY + claimH
                && node != null && Evolution.canUnlock(origin, node, ClientState.DATA)) {
            Network.CHANNEL.sendToServer(new Packets.UnlockNode(node.id()));
            return true;
        }

        int viewportTop = listY + 39;
        int viewportBottom = listY + listH - 8;
        if (mouseX >= listX + 6 && mouseX < listX + listW - 6
                && mouseY >= viewportTop && mouseY < viewportBottom) {
            int index = (int) ((mouseY - viewportTop + listScroll) / 53);
            if (index >= 0 && index < origin.nodes().size()) {
                selectedId = origin.nodes().get(index).id();
                detailScroll = 0;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (origin != null && mouseX >= listX && mouseX <= listX + listW
                && mouseY >= listY && mouseY <= listY + listH) {
            listScroll = Math.max(0, Math.min(maxListScroll(), listScroll - (int) (delta * 48)));
            return true;
        }
        EvolutionNode node = selected();
        if (node != null && mouseX >= detailX && mouseX <= detailX + detailW
                && mouseY >= detailY && mouseY <= detailY + detailH) {
            detailScroll = Math.max(0, Math.min(maxDetailScroll(node), detailScroll - (int) (delta * 40)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 256 || KeyBinds.EVOLUTIONS.matches(key, scanCode)) {
            onClose();
            return true;
        }
        if (origin != null && (key == 264 || key == 265) && !origin.nodes().isEmpty()) {
            int index = 0;
            for (int i = 0; i < origin.nodes().size(); i++) if (origin.nodes().get(i).id().equals(selectedId)) index = i;
            index = key == 264 ? Math.min(origin.nodes().size() - 1, index + 1) : Math.max(0, index - 1);
            selectedId = origin.nodes().get(index).id();
            detailScroll = 0;
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
