package com.lineage.command;

import com.lineage.ability.AbilityManager;
import com.lineage.data.LineageData;
import com.lineage.network.Network;
import com.lineage.origin.OriginManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * /lineage set <players> <origin>
 * /lineage reset <players>          (re-opens the selection screen)
 * /lineage points <players> <n>     (grant evolution points)
 */
public final class LineageCommand {
    private LineageCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lineage")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("origin", ResourceLocationArgument.id())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggestResource(OriginManager.ids(), b))
                                        .executes(LineageCommand::set))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(LineageCommand::reset)))
                .then(Commands.literal("points")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(LineageCommand::points)))));
    }

    private static int set(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "targets");
        ResourceLocation id = ResourceLocationArgument.getId(c, "origin");
        if (OriginManager.get(id) == null) {
            c.getSource().sendFailure(Component.literal("Unknown origin: " + id));
            return 0;
        }
        targets.forEach(p -> AbilityManager.setOrigin(p, id));
        c.getSource().sendSuccess(() -> Component.literal("Set origin of " + targets.size() + " player(s) to " + id), true);
        return targets.size();
    }

    private static int reset(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "targets");
        for (ServerPlayer p : targets) {
            LineageData.get(p).reset();
            AbilityManager.refresh(p);
            Network.sendData(p);
        }
        c.getSource().sendSuccess(() -> Component.literal("Reset " + targets.size() + " player(s)"), true);
        return targets.size();
    }

    private static int points(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "targets");
        int amount = IntegerArgumentType.getInteger(c, "amount");
        for (ServerPlayer p : targets) {
            LineageData.get(p).addPoints(amount);
            Network.sendData(p);
        }
        c.getSource().sendSuccess(() -> Component.literal("Gave " + amount + " evolution point(s) to " + targets.size() + " player(s)"), true);
        return targets.size();
    }
}
