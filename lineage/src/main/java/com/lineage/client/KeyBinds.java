package com.lineage.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class KeyBinds {
    public static final String CATEGORY = "key.categories.lineage";

    public static final KeyMapping[] ABILITY = {
            new KeyMapping("key.lineage.ability1", GLFW.GLFW_KEY_Z, CATEGORY),
            new KeyMapping("key.lineage.ability2", GLFW.GLFW_KEY_X, CATEGORY),
            new KeyMapping("key.lineage.ability3", GLFW.GLFW_KEY_C, CATEGORY),
            new KeyMapping("key.lineage.ability4", GLFW.GLFW_KEY_R, CATEGORY),
            new KeyMapping("key.lineage.ability5", GLFW.GLFW_KEY_G, CATEGORY),
            new KeyMapping("key.lineage.ability6", GLFW.GLFW_KEY_H, CATEGORY),
            new KeyMapping("key.lineage.ability7", GLFW.GLFW_KEY_J, CATEGORY),
            new KeyMapping("key.lineage.ability8", GLFW.GLFW_KEY_K, CATEGORY)
    };
    public static final KeyMapping EVOLUTIONS = new KeyMapping("key.lineage.evolutions", GLFW.GLFW_KEY_V, CATEGORY);

    private KeyBinds() {}
}
