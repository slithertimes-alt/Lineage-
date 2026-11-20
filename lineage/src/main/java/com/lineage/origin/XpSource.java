package com.lineage.origin;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Where evolution XP comes from.
 *  { "type": "kill", "entity": "minecraft:drowned" | "#minecraft:undead" | "any", "amount": 10 }
 *  { "type": "tick", "condition": "in_water", "amount": 1 }   (awarded once per second)
 */
public class XpSource {
    public final String type;
    public final int amount;
    public final String condition;
    private final boolean anyEntity;
    private final ResourceLocation entityId;
    private final TagKey<EntityType<?>> entityTag;

    public XpSource(JsonObject json) {
        this.type = GsonHelper.getAsString(json, "type");
        if (!type.equals("kill") && !type.equals("tick")) {
            throw new JsonSyntaxException("Unknown xp source type: " + type);
        }
        this.amount = GsonHelper.getAsInt(json, "amount", 1);
        this.condition = GsonHelper.getAsString(json, "condition", "always");
        String e = GsonHelper.getAsString(json, "entity", "any");
        if (e.equals("any")) {
            anyEntity = true;
            entityId = null;
            entityTag = null;
        } else if (e.startsWith("#")) {
            anyEntity = false;
            entityId = null;
            entityTag = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(e.substring(1)));
        } else {
            anyEntity = false;
            entityId = new ResourceLocation(e);
            entityTag = null;
        }
    }

    public boolean matchesKill(LivingEntity victim) {
        if (anyEntity) return true;
        if (entityTag != null) return victim.getType().is(entityTag);
        return entityId.equals(ForgeRegistries.ENTITY_TYPES.getKey(victim.getType()));
    }
}
