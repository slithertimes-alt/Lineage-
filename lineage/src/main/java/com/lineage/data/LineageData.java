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

/** Per-player Lineage state: chosen origin, evolution XP/points, unlocked nodes, cooldowns. */
public class LineageData {
    public static final Capability<LineageData> CAP = CapabilityManager.get(new CapabilityToken<>() {});

    private ResourceLocation origin;
    private int xp;
    private int points;
    private final Set<String> unlocked = new LinkedHashSet<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    /** Server-side: true when XP changed and a client sync is pending. */
    public boolean dirty;

    public static LineageData get(Player p) {
        return p.getCapability(CAP).orElseThrow(() -> new IllegalStateException("Lineage capability missing"));
    }

    public ResourceLocation getOrigin() { return origin; }
    public void setOrigin(ResourceLocation id) { this.origin = id; }
    public int getXp() { return xp; }
    public int getPoints() { return points; }
    public void addPoints(int n) { points = Math.max(0, points + n); }
    public void spendPoints(int n) { points = Math.max(0, points - n); }
    public boolean hasNode(String id) { return unlocked.contains(id); }
    public void unlock(String id) { unlocked.add(id); }
    public Set<String> unlockedIds() { return Collections.unmodifiableSet(unlocked); }

    public long cooldownEnd(String abilityId) { return cooldowns.getOrDefault(abilityId, 0L); }
    public void setCooldown(String abilityId, long endTick) { cooldowns.put(abilityId, endTick); }

    /** Adds XP and returns how many evolution points were gained. */
    public int addXp(int amount, int xpPerPoint) {
        xp += amount;
        int gained = 0;
        while (xp >= xpPerPoint) {
            xp -= xpPerPoint;
            points++;
            gained++;
        }
        return gained;
    }

    public void reset() {
        origin = null;
        xp = 0;
        points = 0;
        unlocked.clear();
        cooldowns.clear();
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
    }

    public void copyFrom(LineageData other) {
        load(other.save());
    }
}
