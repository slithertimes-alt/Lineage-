package com.lineage.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** String conditions usable from JSON. Prefix with ! to negate (e.g. "!in_water"). */
public final class Conditions {
    private Conditions() {}

    public static boolean test(String expr, ServerPlayer p) {
        if (expr == null || expr.isEmpty()) return true;
        boolean negate = expr.startsWith("!");
        String c = negate ? expr.substring(1) : expr;
        boolean result = switch (c) {
            case "always" -> true;
            case "in_water" -> p.isInWaterOrBubble();
            case "in_lava" -> p.isInLava();
            case "in_rain" -> p.level().isRainingAt(p.blockPosition());
            case "wet" -> p.isInWaterRainOrBubble();
            case "sunlight" -> p.level().isDay() && p.level().canSeeSky(p.blockPosition());
            case "night" -> p.level().isNight();
            case "in_nether" -> p.level().dimension() == Level.NETHER;
            case "in_end" -> p.level().dimension() == Level.END;
            case "underground" -> p.getY() < 50 && !p.level().canSeeSky(p.blockPosition());
            case "on_fire" -> p.isOnFire();
            case "sprinting" -> p.isSprinting();
            case "sneaking" -> p.isShiftKeyDown();
            case "airborne" -> !p.onGround();
            case "low_health" -> p.getHealth() <= p.getMaxHealth() / 3f;
            default -> false;
        };
        return negate != result;
    }
}
