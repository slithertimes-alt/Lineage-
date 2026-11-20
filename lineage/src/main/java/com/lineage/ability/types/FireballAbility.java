package com.lineage.ability.types;

import com.google.gson.JsonObject;
import com.lineage.ability.Ability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Active: shoot a fireball. JSON: power (0 = small fireball, >0 = large explosive fireball). */
public class FireballAbility extends Ability {
    private final int power;

    public FireballAbility(JsonObject json) {
        super(json);
        this.power = GsonHelper.getAsInt(json, "power", 0);
    }

    @Override
    public boolean isActive() { return true; }

    @Override
    public boolean activate(ServerPlayer p) {
        Level level = p.level();
        Vec3 look = p.getLookAngle();
        Entity fireball;
        if (power > 0) fireball = new LargeFireball(level, p, look.x, look.y, look.z, power);
        else fireball = new SmallFireball(level, p, look.x, look.y, look.z);
        fireball.setPos(p.getX() + look.x * 1.2, p.getEyeY() - 0.2 + look.y * 1.2, p.getZ() + look.z * 1.2);
        level.addFreshEntity(fireball);
        return true;
    }
}
