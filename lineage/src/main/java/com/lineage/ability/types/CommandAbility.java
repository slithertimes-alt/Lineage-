package com.lineage.ability.types;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lineage.ability.Ability;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Active: run vanilla commands as the player (permission level 2). The ultimate customization hook:
 * anything a command or function can do, an ability can do. JSON: "command" (string) or "commands" (array).
 */
public class CommandAbility extends Ability {
    private final List<String> commands = new ArrayList<>();

    public CommandAbility(JsonObject json) {
        super(json);
        if (json.has("commands")) {
            for (JsonElement e : GsonHelper.getAsJsonArray(json, "commands")) commands.add(e.getAsString());
        }
        if (json.has("command")) commands.add(GsonHelper.getAsString(json, "command"));
    }

    @Override
    public boolean isActive() { return true; }

    @Override
    public boolean activate(ServerPlayer p) {
        CommandSourceStack src = p.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        for (String cmd : commands) p.server.getCommands().performPrefixedCommand(src, cmd);
        return true;
    }
}
