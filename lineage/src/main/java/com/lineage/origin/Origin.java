package com.lineage.origin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.lineage.ability.Abilities;
import com.lineage.ability.Ability;
import com.lineage.data.LineageData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record Origin(ResourceLocation id, String name, String description, ResourceLocation icon, int impact,
                     List<Ability> abilities, List<EvolutionNode> nodes) {

    public static Origin fromJson(ResourceLocation id, JsonObject json) {
        List<Ability> abilities = new ArrayList<>();
        for (JsonElement e : GsonHelper.getAsJsonArray(json, "abilities", new JsonArray())) {
            abilities.add(Abilities.parse(GsonHelper.convertToJsonObject(e, "ability")));
        }
        List<EvolutionNode> nodes = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (JsonElement e : GsonHelper.getAsJsonArray(json, "evolutions", new JsonArray())) {
            EvolutionNode n = EvolutionNode.fromJson(GsonHelper.convertToJsonObject(e, "evolution"));
            if (!ids.add(n.id())) throw new JsonSyntaxException("Duplicate evolution id: " + n.id());
            nodes.add(n);
        }
        for (EvolutionNode n : nodes) {
            for (String r : n.requires()) if (!ids.contains(r)) throw new JsonSyntaxException(n.id() + " requires unknown node " + r);
            for (String x : n.excludes()) if (!ids.contains(x)) throw new JsonSyntaxException(n.id() + " excludes unknown node " + x);
        }
        return new Origin(id,
                GsonHelper.getAsString(json, "name", id.getPath()),
                GsonHelper.getAsString(json, "description", ""),
                new ResourceLocation(GsonHelper.getAsString(json, "icon", "minecraft:paper")),
                Mth.clamp(GsonHelper.getAsInt(json, "impact", 1), 1, 3),
                abilities, nodes);
    }

    public EvolutionNode node(String nodeId) {
        for (EvolutionNode n : nodes) if (n.id().equals(nodeId)) return n;
        return null;
    }

    /** Base abilities plus everything granted by unlocked evolution nodes, in a stable order. */
    public List<Ability> abilitiesFor(LineageData data) {
        List<Ability> all = new ArrayList<>(abilities);
        for (EvolutionNode n : nodes) if (data.hasNode(n.id())) all.addAll(n.abilities());
        return all;
    }

    public List<Ability> activeAbilitiesFor(LineageData data) {
        return abilitiesFor(data).stream().filter(Ability::isActive).toList();
    }

    public ItemStack iconStack() { return Ability.stackOf(icon); }
}
