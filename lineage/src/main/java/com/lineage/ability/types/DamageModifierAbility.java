package com.lineage.ability.types;

import com.google.gson.JsonObject;
import com.lineage.ability.Ability;
import com.lineage.ability.Conditions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/**
 * Multiplies incoming damage. JSON: damage_type ("minecraft:fall" or a tag like "#minecraft:is_fire"),
 * multiplier (0 = immune, 1.5 = weakness), condition.
 */
public class DamageModifierAbility extends Ability {
    private final ResourceKey<DamageType> key;
    private final TagKey<DamageType> tag;
    private final float multiplier;
    private final String condition;

    public DamageModifierAbility(JsonObject json) {
        super(json);
        String t = GsonHelper.getAsString(json, "damage_type");
        if (t.startsWith("#")) {
            this.tag = TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(t.substring(1)));
            this.key = null;
        } else {
            this.key = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(t));
            this.tag = null;
        }
        this.multiplier = GsonHelper.getAsFloat(json, "multiplier", 1f);
        this.condition = GsonHelper.getAsString(json, "condition", "always");
    }

    @Override
    public float modifyDamage(ServerPlayer player, DamageSource source, float amount) {
        boolean match = tag != null ? source.is(tag) : source.is(key);
        if (match && Conditions.test(condition, player)) return amount * multiplier;
        return amount;
    }
}
