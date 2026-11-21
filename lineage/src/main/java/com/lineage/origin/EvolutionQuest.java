package com.lineage.origin;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

/** A data-defined objective required to unlock an evolution. */
public final class EvolutionQuest {
    public final String id;
    public final String type;
    public final String description;
    public final int amount;
    public final String condition;
    private final ResourceLocation target;
    private final TagKey<EntityType<?>> entityTag;
    private final boolean anyEntity;

    private EvolutionQuest(String id, String type, String description, int amount, String condition,
                           ResourceLocation target, TagKey<EntityType<?>> entityTag, boolean anyEntity) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.amount = Math.max(1, amount);
        this.condition = condition;
        this.target = target;
        this.entityTag = entityTag;
        this.anyEntity = anyEntity;
    }

    public static EvolutionQuest fromJson(JsonObject json, int index) {
        String type = GsonHelper.getAsString(json, "type");
        String id = GsonHelper.getAsString(json, "id", "quest_" + index);
        String description = GsonHelper.getAsString(json, "description", defaultDescription(json, type));
        String condition = GsonHelper.getAsString(json, "condition", "always");
        int amount = Math.max(1, GsonHelper.getAsInt(json, "amount",
                GsonHelper.getAsInt(json, "seconds", 1)));
        String rawTarget = switch (type) {
            case "kill" -> GsonHelper.getAsString(json, "entity", "any");
            case "visit_dimension" -> GsonHelper.getAsString(json, "dimension", "minecraft:overworld");
            case "hold_item" -> GsonHelper.getAsString(json, "item", "minecraft:paper");
            case "condition_time" -> "";
            default -> throw new JsonSyntaxException("Unknown evolution quest type: " + type);
        };

        if (type.equals("kill") && rawTarget.equals("any")) {
            return new EvolutionQuest(id, type, description, amount, condition, null, null, true);
        }
        if (type.equals("kill") && rawTarget.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(rawTarget.substring(1));
            if (tagId == null) throw new JsonSyntaxException("Invalid entity tag in evolution quest: " + rawTarget);
            return new EvolutionQuest(id, type, description, amount, condition, null,
                    TagKey.create(Registries.ENTITY_TYPE, tagId), false);
        }

        ResourceLocation target = type.equals("condition_time") ? null : ResourceLocation.tryParse(rawTarget);
        if (!type.equals("condition_time") && target == null) {
            throw new JsonSyntaxException("Invalid target in evolution quest: " + rawTarget);
        }
        if (type.equals("hold_item") && !ForgeRegistries.ITEMS.containsKey(target)) {
            throw new JsonSyntaxException("Unknown item in evolution quest: " + target);
        }
        if (type.equals("kill") && !ForgeRegistries.ENTITY_TYPES.containsKey(target)) {
            throw new JsonSyntaxException("Unknown entity in evolution quest: " + target);
        }
        return new EvolutionQuest(id, type, description, amount, condition, target, null, false);
    }

    private static String defaultDescription(JsonObject json, String type) {
        return switch (type) {
            case "kill" -> "Defeat " + GsonHelper.getAsInt(json, "amount", 1) + " target(s)";
            case "visit_dimension" -> "Visit " + GsonHelper.getAsString(json, "dimension", "minecraft:overworld");
            case "hold_item" -> "Carry " + GsonHelper.getAsInt(json, "amount", 1) + " "
                    + GsonHelper.getAsString(json, "item", "minecraft:paper");
            case "condition_time" -> "Spend " + GsonHelper.getAsInt(json, "seconds", 1)
                    + " seconds while " + GsonHelper.getAsString(json, "condition", "always");
            default -> type;
        };
    }

    public boolean matchesKill(LivingEntity victim) {
        if (!type.equals("kill")) return false;
        if (anyEntity) return true;
        if (entityTag != null) return victim.getType().is(entityTag);
        return target != null && target.equals(ForgeRegistries.ENTITY_TYPES.getKey(victim.getType()));
    }

    public boolean matchesDimension(ResourceLocation dimension) {
        return type.equals("visit_dimension") && target != null && target.equals(dimension);
    }

    public Item item() {
        return type.equals("hold_item") && target != null ? ForgeRegistries.ITEMS.getValue(target) : null;
    }

}
