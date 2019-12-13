package io.github.indicode.fabric.permissions.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.indicode.fabric.permissions.Thimble;
import io.github.voidpointerdev.minecraft.offlineinfo.OfflineInfo;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.UUID;
import java.util.function.Predicate;


/**
 * @author Indigo Amann
 */
public class PermissionCommand {
    public static final SuggestionProvider SUGGESTIONS_BUILDER = (source, builder) -> {
        Thimble.PERMISSIONS.getRegisteredPermissions().forEach(it -> {
            String[] inputted = builder.getRemaining().split("[.]");
            String[] perm = it.split("[.]");
            for (int i = 0; i < perm.length; i++) {
                String permP = perm[i];
                if (permP.startsWith(inputted[0])) {
                    if (i + inputted.length <= perm.length && perm[i + inputted.length - 1].startsWith(inputted[inputted.length - 1])) {
                        if (i + inputted.length == perm.length && it.contains(builder.getRemaining())) {
                            builder.suggest(it);
                        } else if (builder.getRemaining().endsWith(".") && it.length() - builder.getRemaining().length() > 0 && !it.substring(builder.getRemaining().length() + 1).contains(".")) {
                            builder.suggest(it);
                        }
                        break;
                    }
                }
            }
        });
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode permission = permissionArgumentBuilder("permission").build();
        CommandNode allPermission = CommandManager.literal("*").build();
        CommandNode players = CommandManager.argument("players", EntityArgumentType.players()).build();
        CommandNode player = OfflineInfo.offlinePlayerArgumentBuilder("player").build();
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal("permission").requires(source -> Thimble.hasPermissionOrOp(source, "thimble.reload", 2) || Thimble.hasPermissionOrOp(source, "thimble.check", 2) || Thimble.hasPermissionOrOp(source, "thimble.modify", 4));

        // Check
        builder.then(
                CommandManager.literal("check")
                        .requires(source -> Thimble.hasPermissionOrOp(source, "thimble.check", 2) || Thimble.hasPermissionOrOp(source, "thimble.modify", 4))
                        .then(
                                player.createBuilder()
                                        .then(
                                                permission.createBuilder().executes(PermissionCommand::checkPerm))
                                        .then(
                                                allPermission.createBuilder().executes(context -> checkPerm(context, "*"))))
        );
        Predicate<ServerCommandSource> modifyPredicate = source -> Thimble.hasPermissionOrOp(source, "thimble.modify", 4);
        // Set
        builder.then(
                CommandManager.literal("set")
                        .requires(modifyPredicate)
                        .then(
                                players.createBuilder()
                                        .then(
                                            permission.createBuilder()
                                                .then(
                                                        CommandManager.argument("enabled", BoolArgumentType.bool())
                                                            .executes(context -> setPerm(context, BoolArgumentType.getBool(context, "enabled")))))
                                        .then(
                                            allPermission.createBuilder()
                                                    .then(
                                                            CommandManager.argument("enabled", BoolArgumentType.bool())
                                                                    .executes(context -> setPerm(context, BoolArgumentType.getBool(context, "enabled"), "*")))))
                        .then(
                                player.createBuilder()
                                        .then(
                                                permission.createBuilder()
                                                        .then(
                                                                CommandManager.argument("enabled", BoolArgumentType.bool())
                                                                        .executes(context -> setPermOffline(context, BoolArgumentType.getBool(context, "enabled")))))
                                        .then(
                                                allPermission.createBuilder()
                                                        .then(
                                                                CommandManager.argument("enabled", BoolArgumentType.bool())
                                                                        .executes(context -> setPermOffline(context, BoolArgumentType.getBool(context, "enabled"), "*")))))
        );
        // Grant
        builder.then(
                CommandManager.literal("grant")
                        .requires(modifyPredicate)
                        .then(
                                players.createBuilder()
                                        .then(
                                                permission.createBuilder()
                                                        .executes(context -> setPerm(context, true)))
                                        .then(
                                                allPermission.createBuilder()
                                                        .executes(context -> setPerm(context, true, "*"))))
                        .then(
                                player.createBuilder()
                                        .then(
                                                permission.createBuilder()
                                                        .executes(context -> setPermOffline(context, true)))
                                        .then(
                                                allPermission.createBuilder()
                                                        .executes(context -> setPermOffline(context, true, "*"))))
        );
        // Revoke
        builder.then(
                CommandManager.literal("revoke")
                        .requires(modifyPredicate)
                        .then(
                                players.createBuilder()
                                        .then(
                                                permission.createBuilder()
                                                        .executes(context -> setPerm(context, false)))
                                        .then(
                                                allPermission.createBuilder()
                                                        .executes(context -> setPerm(context, false, "*"))))
                        .then(
                                player.createBuilder()
                                        .then(
                                                permission.createBuilder()
                                                        .executes(context -> setPermOffline(context, false)))
                                        .then(
                                                allPermission.createBuilder()
                                                        .executes(context -> setPermOffline(context, false, "*"))))
        );
        // Reload
        builder.then(
                CommandManager.literal("reload")
                        .requires(source -> Thimble.hasPermissionOrOp(source, "thimble.reload", 2))
                        .executes(context -> {
                            context.getSource().sendFeedback(new LiteralText("Reloading permissions"), true);
                            Thimble.reload();
                            return 0;
                        })
        );
        LiteralCommandNode<ServerCommandSource> node = dispatcher.register(builder);
        LiteralArgumentBuilder<ServerCommandSource> thimble = CommandManager.literal("thimble");
        thimble.redirect(node);
        dispatcher.register(thimble);
    }

    public static int checkPerm(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return checkPerm(context, StringArgumentType.getString(context, "permission"));
    }
    public static int checkPerm(CommandContext<ServerCommandSource> context, String permission) throws CommandSyntaxException {
        UUID playerID = OfflineInfo.getUUID(context, "player");
        if (playerID == null) {
            context.getSource().sendFeedback(new LiteralText("That is not a valid player").formatted(Formatting.RED), false);
            return 0;
        }
        String playerName = StringArgumentType.getString(context, "player");
        boolean hasPerm = Thimble.PERMISSIONS.hasPermission(permission, playerID);
        context.getSource().sendFeedback(new LiteralText(playerName + " " + (hasPerm ? "has" : "does not have") + " the permission \"" + permission + "\""), false);
        return 1;
    }
    public static int setPerm(CommandContext<ServerCommandSource> context, boolean enabled) throws CommandSyntaxException {
        return setPerm(context, enabled, StringArgumentType.getString(context, "permission"));
    }
    public static int setPerm(CommandContext<ServerCommandSource> context, boolean enabled, String permission) throws CommandSyntaxException {
        for (ServerPlayerEntity player : EntityArgumentType.getPlayers(context, "players")) {
            if (Thimble.PERMISSIONS.defaultPermissionMatches(permission)) {
                context.getSource().sendFeedback(new LiteralText("You cannot modify the default permission").formatted(Formatting.DARK_RED), false);
                return 0;
            }
            boolean hasPerm = Thimble.PERMISSIONS.hasPermission(permission, player.getGameProfile().getId());
            if (hasPerm == enabled) {
                context.getSource().sendFeedback(new LiteralText(player.getGameProfile().getName() + " " + (enabled ? "already has" : "never had") + " the permission \"" + permission + "\"").formatted(Formatting.RED), false);
            } else {
                context.getSource().sendFeedback(new LiteralText(player.getGameProfile().getName() + " " + (enabled ? "has been granted" : "no longer has") + " the permission \"" + permission + "\"").formatted(Formatting.GREEN), false);
                context.getSource().getMinecraftServer().sendMessage(new LiteralText("").append(context.getSource().getDisplayName()).append(new LiteralText(" has " + (enabled ? "granted" : "revoked") + " the permission \"" + permission + "\" for player " + player.getGameProfile().getName())));
                if (!context.getSource().getName().equals(player.getGameProfile().getName()))
                    player.sendMessage(new LiteralText(context.getSource().getName() + " " + (enabled ? "has given you the" : "has taken away your") + " \"" + permission + "\" permission.").formatted(enabled ? Formatting.GREEN : Formatting.RED));
                if (enabled) Thimble.PERMISSIONS.getPlayer(player.getGameProfile().getId()).permission(permission);
                else Thimble.PERMISSIONS.getPlayer(player.getGameProfile().getId()).removePermission(permission);
                Thimble.PERMISSIONS.updatePermissionStateHandlers(permission, player);
            }
        }
        return 1;
    }

    public static int setPermOffline(CommandContext<ServerCommandSource> context, boolean enabled) throws CommandSyntaxException {
        return setPermOffline(context, enabled, StringArgumentType.getString(context, "permission"));
    }
    public static int setPermOffline(CommandContext<ServerCommandSource> context, boolean enabled, String permission) throws CommandSyntaxException {
        UUID playerID = OfflineInfo.getUUID(context, "player");
        if (playerID == null) {
            context.getSource().sendFeedback(new LiteralText("That is not a valid player").formatted(Formatting.RED), false);
            return 0;
        }
        String playerName = StringArgumentType.getString(context, "player");
        ServerPlayerEntity playerEntity = OfflineInfo.getPlayerEntity(context, "player");
        if (Thimble.PERMISSIONS.defaultPermissionMatches(permission)) {
            context.getSource().sendFeedback(new LiteralText("You cannot modify the default permission").formatted(Formatting.DARK_RED), false);
            return 0;
        }
        boolean hasPerm = Thimble.PERMISSIONS.hasPermission(permission, playerID);
        if (hasPerm == enabled) {
            context.getSource().sendFeedback(new LiteralText(playerName + " " + (enabled ? "already has" : "never had") + " the permission \"" + permission + "\"").formatted(Formatting.RED), false);
        } else {
            context.getSource().sendFeedback(new LiteralText(playerName + " " + (enabled ? "has been granted" : "no longer has") + " the permission \"" + permission + "\"").formatted(Formatting.GREEN), false);
            context.getSource().getMinecraftServer().sendMessage(new LiteralText("").append(context.getSource().getDisplayName()).append(new LiteralText(" has " + (enabled ? "granted" : "revoked") + " the permission \"" + permission + "\" for player " + playerName)));
            if (playerEntity != null && !context.getSource().getName().equals(playerName))
                playerEntity.sendMessage(new LiteralText(context.getSource().getName() + " " + (enabled ? "has given you the" : "has taken away your") + " \"" + permission + "\" permission.").formatted(enabled ? Formatting.GREEN : Formatting.RED));
            if (enabled) Thimble.PERMISSIONS.getPlayer(playerID).permission(permission);
            else Thimble.PERMISSIONS.getPlayer(playerID).removePermission(permission);
            if (playerEntity != null) Thimble.PERMISSIONS.updatePermissionStateHandlers(permission, playerEntity);
        }
        return 1;
    }

    public static ArgumentBuilder permissionArgumentBuilder(String name) {
        RequiredArgumentBuilder<ServerCommandSource, String> builder = RequiredArgumentBuilder.argument(name, StringArgumentType.word());
        builder.suggests(SUGGESTIONS_BUILDER);
        return builder;
    }
}
