package com.lineage.origin;

import com.lineage.ability.Conditions;
import com.lineage.data.LineageData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

/** Quest rules shared with the evolution UI, plus server-side objective tracking. */
public final class Evolution {
    private Evolution() {}

    /** True if another unlocked node excludes this one, or this node excludes an unlocked one. */
    public static boolean isBlocked(Origin origin, EvolutionNode node, LineageData data) {
        for (String excluded : node.excludes()) if (data.hasNode(excluded)) return true;
        for (String id : data.unlockedIds()) {
            EvolutionNode unlocked = origin.node(id);
            if (unlocked != null && unlocked.excludes().contains(node.id())) return true;
        }
        return false;
    }

    public static boolean prerequisitesMet(EvolutionNode node, LineageData data) {
        for (String required : node.requires()) if (!data.hasNode(required)) return false;
        return true;
    }

    public static String questKey(EvolutionNode node, EvolutionQuest quest) {
        return node.id() + "/" + quest.id;
    }

    public static int questProgress(EvolutionNode node, EvolutionQuest quest, LineageData data) {
        return Math.min(quest.amount, data.questProgress(questKey(node, quest)));
    }

    public static int completedQuestCount(EvolutionNode node, LineageData data) {
        int completed = 0;
        for (EvolutionQuest quest : node.quests()) {
            if (questProgress(node, quest, data) >= quest.amount) completed++;
        }
        return completed;
    }

    public static boolean questsComplete(EvolutionNode node, LineageData data) {
        return completedQuestCount(node, data) == node.quests().size();
    }

    public static boolean canUnlock(Origin origin, EvolutionNode node, LineageData data) {
        return !data.hasNode(node.id()) && prerequisitesMet(node, data)
                && !isBlocked(origin, node, data) && questsComplete(node, data);
    }

    private static Origin current(ServerPlayer player) {
        return OriginManager.get(LineageData.get(player).getOrigin());
    }

    /** Called when the player kills a living entity. */
    public static void onKill(ServerPlayer player, LivingEntity victim) {
        Origin origin = current(player);
        if (origin == null) return;
        LineageData data = LineageData.get(player);
        boolean changed = false;
        for (EvolutionNode node : origin.nodes()) {
            if (data.hasNode(node.id())) continue;
            for (EvolutionQuest quest : node.quests()) {
                if (quest.matchesKill(victim)) {
                    String key = questKey(node, quest);
                    int before = data.questProgress(key);
                    data.addQuestProgress(key, 1, quest.amount);
                    changed |= data.questProgress(key) != before;
                }
            }
        }
        if (changed) data.dirty = true;
    }

    /** Called once per second; updates timed goals and held-item goals. */
    public static void tickSources(ServerPlayer player) {
        Origin origin = current(player);
        if (origin == null) return;
        LineageData data = LineageData.get(player);
        boolean changed = false;
        for (EvolutionNode node : origin.nodes()) {
            if (data.hasNode(node.id())) continue;
            for (EvolutionQuest quest : node.quests()) {
                String key = questKey(node, quest);
                int next = data.questProgress(key);
                if (quest.type.equals("condition_time") && Conditions.test(quest.condition, player)) {
                    next = Math.min(quest.amount, next + 1);
                } else if (quest.type.equals("hold_item")) {
                    Item item = quest.item();
                    next = item == null ? 0 : Math.min(quest.amount, player.getInventory().countItem(item));
                }
                changed |= data.setQuestProgress(key, next);
            }
        }
        if (changed) data.dirty = true;
    }

    /** Called when a player enters a new dimension. */
    public static void onDimensionChange(ServerPlayer player, ResourceLocation dimension) {
        Origin origin = current(player);
        if (origin == null) return;
        LineageData data = LineageData.get(player);
        boolean changed = false;
        for (EvolutionNode node : origin.nodes()) {
            if (data.hasNode(node.id())) continue;
            for (EvolutionQuest quest : node.quests()) {
                if (quest.matchesDimension(dimension)) {
                    changed |= data.setQuestProgress(questKey(node, quest), quest.amount);
                }
            }
        }
        if (changed) data.dirty = true;
    }
}
