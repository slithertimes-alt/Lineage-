package com.lineage.ability.types;

import com.google.gson.JsonObject;
import com.lineage.ability.Abilities;
import com.lineage.ability.Ability;
import com.lineage.ability.Conditions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;

/** Passive potion effect while a condition holds. JSON: effect, amplifier, duration (ticks), condition. */
public class EffectAbility extends Ability {
    private final MobEffect effect;
    private final int amplifier;
    private final int duration;
    private final String condition;

    public EffectAbility(JsonObject json) {
        super(json);
        this.effect = lookup(ForgeRegistries.MOB_EFFECTS, GsonHelper.getAsString(json, "effect"), "effect");
        this.amplifier = GsonHelper.getAsInt(json, "amplifier", 0);
        this.duration = GsonHelper.getAsInt(json, "duration", 40);
        this.condition = GsonHelper.getAsString(json, "condition", "always");
    }

    @Override
    public void tick(ServerPlayer player) {
        if (Conditions.test(condition, player)) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, true));
        }
    }
}
