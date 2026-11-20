package com.lineage.ability.types;

import com.google.gson.JsonObject;
import com.lineage.ability.Ability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.Vec3;

/** Active: launch the player where they look. JSON: strength, upward. */
public class DashAbility extends Ability {
    private final double strength;
    private final double upward;

    public DashAbility(JsonObject json) {
        super(json);
        this.strength = GsonHelper.getAsDouble(json, "strength", 1.5);
        this.upward = GsonHelper.getAsDouble(json, "upward", 0.0);
    }

    @Override
    public boolean isActive() { return true; }

    @Override
    public boolean activate(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        p.setDeltaMovement(look.x * strength, look.y * strength + upward, look.z * strength);
        p.hurtMarked = true; // forces velocity sync to the client
        p.resetFallDistance();
        return true;
    }
}
