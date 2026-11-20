package com.lineage.origin;

import com.lineage.Config;
import com.lineage.ability.Conditions;
import com.lineage.data.LineageData;
import com.lineage.network.Network;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/** Evolution rules (shared by client for UI state) and server-side XP awarding. */
public final class Evolution {
    private Evolution() {}

    /** True if another unlocked node excludes this one, or this node excludes an unlocked one. */
    public static boolean isBlocked(Origin o, EvolutionNode n, LineageData d) {
        for (String x : n.excludes()) if (d.hasNode(x)) return true;
        for (String id : d.unlockedIds()) {
            EvolutionNode u = o.node(id);
            if (u != null && u.excludes().contains(n.id())) return true;
        }
        return false;
    }

    public static boolean requirementsMet(EvolutionNode n, LineageData d) {
        for (String r : n.requires()) if (!d.hasNode(r)) return false;
        return true;
    }

    public static boolean canUnlock(Origin o, EvolutionNode n, LineageData d) {
        return !d.hasNode(n.id()) && requirementsMet(n, d) && !isBlocked(o, n, d) && d.getPoints() >= n.cost();
    }

    // ---------------- server side ----------------

    private static Origin current(ServerPlayer p) {
        return OriginManager.get(LineageData.get(p).getOrigin());
    }

    public static void onKill(ServerPlayer p, LivingEntity victim) {
        Origin o = current(p);
        if (o == null) return;
        int total = 0;
        for (XpSource s : o.xpSources()) {
            if (s.type.equals("kill") && s.matchesKill(victim) && Conditions.test(s.condition, p)) total += s.amount;
        }
        if (total > 0) addXp(p, total);
    }

    /** Called once per second. */
    public static void tickSources(ServerPlayer p) {
        Origin o = current(p);
        if (o == null) return;
        int total = 0;
        for (XpSource s : o.xpSources()) {
            if (s.type.equals("tick") && Conditions.test(s.condition, p)) total += s.amount;
        }
        if (total > 0) addXp(p, total);
    }

    public static void addXp(ServerPlayer p, int amount) {
        LineageData d = LineageData.get(p);
        Origin o = OriginManager.get(d.getOrigin());
        if (o == null) return;
        // Stochastic rounding so small multipliers still work over time.
        double scaled = amount * Config.XP_MULTIPLIER.get();
        int whole = (int) scaled;
        int amt = whole + (p.getRandom().nextDouble() < scaled - whole ? 1 : 0);
        if (amt <= 0) return;
        int gained = d.addXp(amt, o.xpPerPoint());
        d.dirty = true;
        if (gained > 0) {
            p.displayClientMessage(Component.literal("\u00a76Evolution point earned! Open your Evolutions menu."), true);
            p.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
            Network.sendData(p);
        }
    }
}
