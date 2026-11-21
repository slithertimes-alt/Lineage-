package com.lineage.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

import java.util.*;

/** Per-player Lineage state: chosen origin, quest counters, unlocked nodes, and cooldowns. */
public class LineageData {
    public static final Capability<LineageData> CAP = CapabilityManager.get(new CapabilityToken<>() {});

    private ResourceLocation origin;
    // Legacy fields remain in player saves for compatibility; quests now gate evolutions.
    private int xp;
    private int points;
    private final Set<String> unlocked = new LinkedHashSet<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<String, Integer> questProgress = new HashMap<>();
    /** Server-side: true when quest progress changed and a client sync is pending. */
    public boolean dirty;

    public static LineageData get(Player p) {
        return p.getCapability(CAP).orElseThrow(() -> new IllegalStateException("Lineage capability missing"));
    }

    public ResourceLocation getOrigin() { return origin; }
    public void setOrigin(ResourceLocation id) { this.origin = id; }
    public boolean hasNode(String id) { return unlocked.contains(id); }
    public void unlock(String id) { unlocked.add(id); }
    public Set<String> unlockedIds() { return Collections.unmodifiableSet(unlocked); }

    public long cooldownEnd(String abilityId) { return cooldowns.getOrDefault(abilityId, 0L); }
    public void setCooldown(String abilityId, long endTick) { cooldowns.put(abilityId, endTick); }

    public int questProgress(String key) { return questProgress.getOrDefault(key, 0); }
    public boolean setQuestProgress(String key, int value) {
        int progress = Math.max(0, value);
        if (questProgress(key) == progress) return false;
        questProgress.put(key, progress);
        return true;
    }
    public void addQuestProgress(String key, int amount, int maximum) {
        setQuestProgress(key, Math.min(maximum, questProgress(key) + amount));
    }

    public void reset() {
        origin = null;
        xp = 0;
        points = 0;
        unlocked.clear();
        cooldowns.clear();
        questProgress.clear();
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("origin", origin == null ? "" : origin.toString());
        t.putInt("xp", xp);
        t.putInt("points", points);
        ListTag list = new ListTag();
        unlocked.forEach(s -> list.add(StringTag.valueOf(s)));
        t.put("unlocked", list);
        CompoundTag cds = new CompoundTag();
        cooldowns.forEach(cds::putLong);
        t.put("cooldowns", cds);
        CompoundTag quests = new CompoundTag();
        questProgress.forEach(quests::putInt);
        t.put("questProgress", quests);
        return t;
    }

    public void load(CompoundTag t) {
        reset();
        String o = t.getString("origin");
        origin = o.isEmpty() ? null : ResourceLocation.tryParse(o);
        xp = t.getInt("xp");
        points = t.getInt("points");
        ListTag list = t.getList("unlocked", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) unlocked.add(list.getString(i));
        CompoundTag cds = t.getCompound("cooldowns");
        for (String k : cds.getAllKeys()) cooldowns.put(k, cds.getLong(k));
        CompoundTag quests = t.getCompound("questProgress");
        for (String k : quests.getAllKeys()) questProgress.put(k, quests.getInt(k));
    }

    public void copyFrom(LineageData other) {
        load(other.save());
    }
}
