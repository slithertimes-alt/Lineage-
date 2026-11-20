package com.lineage;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue XP_MULTIPLIER;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        XP_MULTIPLIER = b.comment("Multiplier applied to all evolution XP gains (0.5 = half speed, 2 = double).")
                .defineInRange("xpMultiplier", 1.0, 0.0, 100.0);
        SPEC = b.build();
    }

    private Config() {}
}
