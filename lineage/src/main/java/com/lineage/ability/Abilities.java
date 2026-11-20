package com.lineage.ability;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.lineage.Lineage;
import com.lineage.ability.types.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Registry of ability types. Other mods/addons can call {@link #register}. */
public final class Abilities {
    private static final Map<ResourceLocation, Function<JsonObject, Ability>> TYPES = new HashMap<>();

    private Abilities() {}

    public static void register(ResourceLocation typeId, Function<JsonObject, Ability> factory) {
        TYPES.put(typeId, factory);
    }

    private static ResourceLocation l(String path) { return new ResourceLocation(Lineage.MODID, path); }

    public static void registerBuiltins() {
        register(l("attribute"), AttributeAbility::new);
        register(l("effect"), EffectAbility::new);
        register(l("damage_modifier"), DamageModifierAbility::new);
        register(l("dash"), DashAbility::new);
        register(l("active_effect"), ActiveEffectAbility::new);
        register(l("fireball"), FireballAbility::new);
        register(l("command"), CommandAbility::new);
    }

    /** "type" may be "dash" (assumes lineage:) or a full "othermod:thing". */
    public static Ability parse(JsonObject json) {
        String t = GsonHelper.getAsString(json, "type");
        ResourceLocation id = t.contains(":") ? new ResourceLocation(t) : l(t);
        Function<JsonObject, Ability> f = TYPES.get(id);
        if (f == null) throw new JsonSyntaxException("Unknown ability type: " + t);
        return f.apply(json);
    }
}
