package com.lineage.ability.types;

import com.google.gson.JsonObject;
import com.lineage.ability.Ability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Active: apply an effect to yourself (target "self") or to nearby entities (target "nearby", radius). */
public class ActiveEffectAbility extends Ability {
    private final MobEffect effect;
    private final int amplifier;
    private final int duration;
    private final boolean nearby;
    private final double radius;

    public ActiveEffectAbility(JsonObject json) {
        super(json);
        this.effect = lookup(ForgeRegistries.MOB_EFFECTS, GsonHelper.getAsString(json, "effect"), "effect");
        this.amplifier = GsonHelper.getAsInt(json, "amplifier", 0);
        this.duration = GsonHelper.getAsInt(json, "duration", 200);
        this.nearby = GsonHelper.getAsString(json, "target", "self").equals("nearby");
        this.radius = GsonHelper.getAsDouble(json, "radius", 5.0);
    }

    @Override
    public boolean isActive() { return true; }

    @Override
    public boolean activate(ServerPlayer p) {
        if (!nearby) {
            p.addEffect(new MobEffectInstance(effect, duration, amplifier));
            return true;
        }
        List<LivingEntity> targets = p.level().getEntitiesOfClass(LivingEntity.class,
                p.getBoundingBox().inflate(radius),
                e -> e != p && e.isAlive() && !e.isAlliedTo(p));
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(effect, duration, amplifier), p);
        }
        return true;
    }
}
