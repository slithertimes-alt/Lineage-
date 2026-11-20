package com.lineage.ability.types;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.lineage.ability.Ability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Passive attribute modifier. JSON: attribute, amount, operation (addition | multiply_base | multiply_total). */
public class AttributeAbility extends Ability {
    private final Attribute attribute;
    private final double amount;
    private final AttributeModifier.Operation operation;
    private final UUID modifierId;

    public AttributeAbility(JsonObject json) {
        super(json);
        String attr = GsonHelper.getAsString(json, "attribute");
        this.attribute = lookup(ForgeRegistries.ATTRIBUTES, attr, "attribute");
        this.amount = GsonHelper.getAsDouble(json, "amount");
        String op = GsonHelper.getAsString(json, "operation", "addition");
        this.operation = switch (op) {
            case "addition" -> AttributeModifier.Operation.ADDITION;
            case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> throw new JsonSyntaxException("Unknown operation: " + op);
        };
        this.modifierId = UUID.nameUUIDFromBytes(("lineage:" + id + ":" + attr).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void onApply(ServerPlayer player) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;
        inst.removeModifier(modifierId);
        // Name must start with "lineage:" so AbilityManager can clear it on refresh.
        inst.addTransientModifier(new AttributeModifier(modifierId, "lineage:" + id, amount, operation));
    }
}
