package com.lineage.ability;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * Base class for every ability. Subclass this, register a factory in {@link Abilities},
 * and your type becomes usable from JSON.
 *
 * Common JSON fields: id, name, description, icon, cooldown (seconds), hunger_cost,
 * use_condition, sound.
 */
public abstract class Ability {
    public final String id;
    public final String name;
    public final String description;
    public final ResourceLocation icon;
    public final int cooldownTicks;
    public final int hungerCost;
    public final String useCondition;
    public final ResourceLocation sound;

    protected Ability(JsonObject json) {
        this.id = GsonHelper.getAsString(json, "id");
        this.name = GsonHelper.getAsString(json, "name", id);
        this.description = GsonHelper.getAsString(json, "description", "");
        this.icon = new ResourceLocation(GsonHelper.getAsString(json, "icon", "minecraft:paper"));
        this.cooldownTicks = Math.max(0, Math.round(GsonHelper.getAsFloat(json, "cooldown", 0f) * 20f));
        this.hungerCost = GsonHelper.getAsInt(json, "hunger_cost", 0);
        this.useCondition = GsonHelper.getAsString(json, "use_condition", "always");
        String s = GsonHelper.getAsString(json, "sound", "");
        this.sound = s.isEmpty() ? null : new ResourceLocation(s);
    }

    /** Active abilities get a keybind slot and a HUD icon. */
    public boolean isActive() { return false; }

    /** Called whenever abilities are (re)applied: login, respawn, origin change, evolution. */
    public void onApply(ServerPlayer player) {}

    /** Called every 10 ticks for passive logic. */
    public void tick(ServerPlayer player) {}

    /** Called when the player presses the ability key. Return true if it fired (starts cooldown). */
    public boolean activate(ServerPlayer player) { return false; }

    /** Modify incoming damage. Return the new amount (0 = immune). */
    public float modifyDamage(ServerPlayer player, DamageSource source, float amount) { return amount; }

    public ItemStack iconStack() { return stackOf(icon); }

    public static ItemStack stackOf(ResourceLocation id) {
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return new ItemStack(item == null || item == Items.AIR ? Items.PAPER : item);
    }

    protected static <T> T lookup(IForgeRegistry<T> registry, String id, String what) {
        ResourceLocation rl = new ResourceLocation(id);
        if (!registry.containsKey(rl)) throw new JsonSyntaxException("Unknown " + what + ": " + id);
        return registry.getValue(rl);
    }
}
