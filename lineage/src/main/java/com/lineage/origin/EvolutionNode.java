package com.lineage.origin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lineage.ability.Abilities;
import com.lineage.ability.Ability;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** One node in an origin's evolution tree. */
public record EvolutionNode(String id, String name, String description, ResourceLocation icon, int cost,
                            List<String> requires, List<String> excludes, List<Ability> abilities) {

    public static EvolutionNode fromJson(JsonObject json) {
        String id = GsonHelper.getAsString(json, "id");
        List<Ability> abilities = new ArrayList<>();
        for (JsonElement e : GsonHelper.getAsJsonArray(json, "abilities", new JsonArray())) {
            abilities.add(Abilities.parse(GsonHelper.convertToJsonObject(e, "ability")));
        }
        return new EvolutionNode(
                id,
                GsonHelper.getAsString(json, "name", id),
                GsonHelper.getAsString(json, "description", ""),
                new ResourceLocation(GsonHelper.getAsString(json, "icon", "minecraft:paper")),
                Math.max(0, GsonHelper.getAsInt(json, "cost", 1)),
                strings(json, "requires"),
                strings(json, "excludes"),
                abilities);
    }

    private static List<String> strings(JsonObject json, String key) {
        List<String> out = new ArrayList<>();
        if (json.has(key)) for (JsonElement e : GsonHelper.getAsJsonArray(json, key)) out.add(e.getAsString());
        return out;
    }

    public ItemStack iconStack() { return Ability.stackOf(icon); }
}
