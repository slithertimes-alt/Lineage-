package com.lineage.ability;

import com.lineage.data.LineageData;
import com.lineage.network.Network;
import com.lineage.origin.Origin;
import com.lineage.origin.OriginManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;

/** Server-side glue between a player's data and their abilities. */
public final class AbilityManager {
    private static final String MODIFIER_PREFIX = "lineage:";

    private AbilityManager() {}

    private static Origin originOf(LineageData d) { return OriginManager.get(d.getOrigin()); }

    public static void setOrigin(ServerPlayer p, ResourceLocation id) {
        LineageData d = LineageData.get(p);
        d.reset();
        d.setOrigin(id);
        refresh(p);
        Network.sendData(p);
    }

    /** Clears all Lineage attribute modifiers and re-applies the current abilities. */
    public static void refresh(ServerPlayer p) {
        clearModifiers(p);
        LineageData d = LineageData.get(p);
        Origin o = originOf(d);
        if (o != null) {
            for (Ability a : o.abilitiesFor(d)) a.onApply(p);
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    private static void clearModifiers(ServerPlayer p) {
        for (Attribute attr : ForgeRegistries.ATTRIBUTES.getValues()) {
            AttributeInstance inst = p.getAttribute(attr);
            if (inst == null) continue;
            List<UUID> ids = inst.getModifiers().stream()
                    .filter(m -> m.getName().startsWith(MODIFIER_PREFIX))
                    .map(AttributeModifier::getId)
                    .toList();
            for (UUID id : ids) inst.removeModifier(id);
        }
    }

    public static void tick(ServerPlayer p) {
        LineageData d = LineageData.get(p);
        Origin o = originOf(d);
        if (o == null) return;
        for (Ability a : o.abilitiesFor(d)) a.tick(p);
    }

    public static float modifyDamage(ServerPlayer p, DamageSource source, float amount) {
        LineageData d = LineageData.get(p);
        Origin o = originOf(d);
        if (o == null) return amount;
        for (Ability a : o.abilitiesFor(d)) amount = a.modifyDamage(p, source, amount);
        return amount;
    }

    /** Player pressed ability key #slot (0-based among their active abilities). */
    public static void use(ServerPlayer p, int slot) {
        LineageData d = LineageData.get(p);
        Origin o = originOf(d);
        if (o == null) return;
        List<Ability> actives = o.activeAbilitiesFor(d);
        if (slot < 0 || slot >= actives.size()) return;
        Ability a = actives.get(slot);

        long now = p.level().getGameTime();
        if (d.cooldownEnd(a.id) > now) {
            p.displayClientMessage(Component.literal("\u00a77" + a.name + " is recharging..."), true);
            return;
        }
        if (!Conditions.test(a.useCondition, p)) {
            p.displayClientMessage(Component.literal("\u00a7cYou can't use " + a.name + " right now."), true);
            return;
        }
        if (a.hungerCost > 0 && p.getFoodData().getFoodLevel() < a.hungerCost) {
            p.displayClientMessage(Component.literal("\u00a7cToo hungry to use " + a.name + "."), true);
            return;
        }
        if (!a.activate(p)) return;

        if (a.hungerCost > 0) p.getFoodData().addExhaustion(a.hungerCost * 4f);
        if (a.cooldownTicks > 0) d.setCooldown(a.id, now + a.cooldownTicks);
        if (a.sound != null) {
            SoundEvent se = ForgeRegistries.SOUND_EVENTS.getValue(a.sound);
            if (se != null) p.level().playSound(null, p.getX(), p.getY(), p.getZ(), se, SoundSource.PLAYERS, 1f, 1f);
        }
        Network.sendData(p);
    }
}
