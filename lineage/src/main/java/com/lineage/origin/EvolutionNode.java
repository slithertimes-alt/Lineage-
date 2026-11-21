package com.lineage.origin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.lineage.ability.Abilities;
import com.lineage.ability.Ability;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** One node in an origin's evolution tree. */
public record EvolutionNode(String id, String name, String description, ResourceLocation icon,
                            List<String> requires, List<String> excludes, List<EvolutionQuest> quests,
                            List<Ability> abilities) {

    public static EvolutionNode fromJson(JsonObject json) {
        String id = GsonHelper.getAsString(json, "id");
        List<Ability> abilities = new ArrayList<>();
        for (JsonElement e : GsonHelper.getAsJsonArray(json, "abilities", new JsonArray())) {
            abilities.add(Abilities.parse(GsonHelper.convertToJsonObject(e, "ability")));
        }
        List<EvolutionQuest> quests = new ArrayList<>();
        List<String> questIds = new ArrayList<>();
        JsonArray questJson = GsonHelper.getAsJsonArray(json, "quests", new JsonArray());
        for (int i = 0; i < questJson.size(); i++) {
            EvolutionQuest quest = EvolutionQuest.fromJson(GsonHelper.convertToJsonObject(questJson.get(i), "quest"), i);
            if (questIds.contains(quest.id)) throw new JsonSyntaxException("Duplicate quest id in evolution " + id + ": " + quest.id);
            questIds.add(quest.id);
            quests.add(quest);
        }
        if (quests.isEmpty()) throw new JsonSyntaxException("Evolution " + id + " must define at least one quest");
        return new EvolutionNode(
                id,
                GsonHelper.getAsString(json, "name", id),
                GsonHelper.getAsString(json, "description", ""),
                new ResourceLocation(GsonHelper.getAsString(json, "icon", "minecraft:paper")),
                strings(json, "requires"),
                strings(json, "excludes"),
                quests,
                abilities);
    }

    private static List<String> strings(JsonObject json, String key) {
        List<String> out = new ArrayList<>();
        if (json.has(key)) for (JsonElement e : GsonHelper.getAsJsonArray(json, key)) out.add(e.getAsString());
        return out;
    }

    public ItemStack iconStack() { return Ability.stackOf(icon); }
}
