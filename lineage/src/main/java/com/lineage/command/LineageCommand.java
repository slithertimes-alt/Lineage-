package com.lineage.command;

import com.lineage.ability.AbilityManager;
import com.lineage.data.LineageData;
import com.lineage.network.Network;
import com.lineage.origin.Evolution;
import com.lineage.origin.EvolutionNode;
import com.lineage.origin.EvolutionQuest;
import com.lineage.origin.Origin;
import com.lineage.origin.OriginManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
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
 * /lineage reset <players>          (clears the origin and progress)
 * /lineage complete <players> <node> (completes that node's quest checklist)
 * /lineage unlock <players> <node>   (debug: grants the evolution immediately)
 */
public final class LineageCommand {
    private LineageCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("lineage");
        root.then(Commands.literal("list").executes(LineageCommand::list));
        root.then(Commands.literal("set")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("origin", ResourceLocationArgument.id())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggestResource(OriginManager.ids(), b))
                                .executes(LineageCommand::set))));
        root.then(Commands.literal("reset")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(LineageCommand::reset)));
        root.then(Commands.literal("complete")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("node", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(nodeIds(), b))
                                .executes(LineageCommand::completeQuests))));
        root.then(Commands.literal("unlock")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("node", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(nodeIds(), b))
                                .executes(LineageCommand::unlock))));
        dispatcher.register(root);
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        StringBuilder message = new StringBuilder("Available Lineage origins:");
        int count = 0;
        for (ResourceLocation id : OriginManager.ids()) {
            message.append(" ").append(id);
            count++;
        }
        if (count == 0) message.append(" none (check datapacks, then run /reload)");
        String result = message.toString();
        c.getSource().sendSuccess(() -> Component.literal(result), false);
        return count;
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

    private static Iterable<String> nodeIds() {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        for (ResourceLocation originId : OriginManager.ids()) {
            Origin origin = OriginManager.get(originId);
            if (origin != null) for (EvolutionNode node : origin.nodes()) ids.add(node.id());
        }
        return ids;
    }

    private static int completeQuests(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "targets");
        String nodeId = StringArgumentType.getString(c, "node");
        int changed = 0;
        for (ServerPlayer player : targets) {
            Origin origin = OriginManager.get(LineageData.get(player).getOrigin());
            EvolutionNode node = origin == null ? null : origin.node(nodeId);
            if (node == null) continue;
            LineageData data = LineageData.get(player);
            for (EvolutionQuest quest : node.quests()) {
                data.setQuestProgress(Evolution.questKey(node, quest), quest.amount);
            }
            data.dirty = true;
            Network.sendData(player);
            changed++;
        }
        if (changed == 0) {
            c.getSource().sendFailure(Component.literal("No target has an evolution named '" + nodeId + "' for their current origin."));
            return 0;
        }
        final int count = changed;
        c.getSource().sendSuccess(() -> Component.literal("Completed quests for '" + nodeId + "' for " + count + " player(s)."), true);
        return count;
    }

    private static int unlock(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "targets");
        String nodeId = StringArgumentType.getString(c, "node");
        int changed = 0;
        for (ServerPlayer player : targets) {
            Origin origin = OriginManager.get(LineageData.get(player).getOrigin());
            EvolutionNode node = origin == null ? null : origin.node(nodeId);
            if (node == null) continue;
            LineageData data = LineageData.get(player);
            for (EvolutionQuest quest : node.quests()) {
                data.setQuestProgress(Evolution.questKey(node, quest), quest.amount);
            }
            data.unlock(node.id());
            AbilityManager.refresh(player);
            Network.sendData(player);
            changed++;
        }
        if (changed == 0) {
            c.getSource().sendFailure(Component.literal("No target has an evolution named '" + nodeId + "' for their current origin."));
            return 0;
        }
        final int count = changed;
        c.getSource().sendSuccess(() -> Component.literal("Force-unlocked '" + nodeId + "' for " + count + " player(s)."), true);
        return count;
    }

}
