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
import io.github.indicode.fabric.permissions.Thimble;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.function.Predicate;


/**
 * @author Indigo Amann
 */
public class PermissionCommand {
    public static final SuggestionProvider SUGGESTIONS_BUILDER = (source, builder) -> {
        Thimble.PERMISSIONS.mapPermissions(Thimble.PERMISSIONS.getRegisteredPermissions()).keySet().forEach(it -> {
            if (it.startsWith(builder.getRemaining())) builder.suggest(it);
        });
        return builder.buildFuture();
    };
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal("permission").requires(source -> Thimble.hasPermissionOrOp(source, "thimble.check", 2) || Thimble.hasPermissionOrOp(source, "thimble.modify", 4));
        {
            LiteralArgumentBuilder<ServerCommandSource> check = CommandManager.literal("check");
            check.requires(source -> Thimble.hasPermissionOrOp(source, "thimble.check", 2) || Thimble.hasPermissionOrOp(source, "thimble.modify", 4));
            ArgumentBuilder player = CommandManager.argument("player", EntityArgumentType.player());
            ArgumentBuilder permission = permissionArgumentBuilder("permission");
            permission.executes(PermissionCommand::checkPerm);
            player.then(permission);
            check.then(player);
            builder.then(check);
        }
        {
            Predicate<ServerCommandSource> modifyPredicate = source -> Thimble.hasPermissionOrOp(source, "thimble.modify", 4);
            LiteralArgumentBuilder<ServerCommandSource> set = CommandManager.literal("set");
            set.requires(modifyPredicate);
            LiteralArgumentBuilder<ServerCommandSource> grant = CommandManager.literal("grant");
            grant.requires(modifyPredicate);
            LiteralArgumentBuilder<ServerCommandSource> revoke = CommandManager.literal("revoke");
            revoke.requires(modifyPredicate);
            ArgumentBuilder player = CommandManager.argument("players", EntityArgumentType.players());
            ArgumentBuilder permission = permissionArgumentBuilder("permission");
            ArgumentBuilder playerGrant = CommandManager.argument("players", EntityArgumentType.players());
            ArgumentBuilder permissionGrant = permissionArgumentBuilder("permission");
            permissionGrant.executes(context -> setPerm(context, true));
            ArgumentBuilder playerRevoke = CommandManager.argument("players", EntityArgumentType.players());
            ArgumentBuilder permissionRevoke = permissionArgumentBuilder("permission");
            permissionRevoke.executes(context -> setPerm(context, false));
            ArgumentBuilder enabled = CommandManager.argument("enabled", BoolArgumentType.bool());
            enabled.executes(context -> setPerm(context, BoolArgumentType.getBool(context, "enabled")));
            permission.then(enabled);
            player.then(permission);
            playerGrant.then(permissionGrant);
            playerRevoke.then(permissionRevoke);
            revoke.then(playerRevoke);
            grant.then(playerGrant);
            set.then(player);
            builder.then(set);
            builder.then(grant);
            builder.then(revoke);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> reload = CommandManager.literal("reload");
            reload.requires(source -> Thimble.hasPermissionOrOp(source, "thimble.reload", 2));
            reload.executes(context -> {
                context.getSource().sendFeedback(new LiteralText("Reloading permissions"), true);
                Thimble.reload();
                return 0;
            });
            builder.then(reload);
        }
        dispatcher.register(builder);
    }
    public static int checkPerm(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        String permission = StringArgumentType.getString(context,"permission");
        boolean hasPerm = Thimble.PERMISSIONS.hasPermission(permission, player.getGameProfile().getId());
        context.getSource().sendFeedback(new LiteralText(player.getGameProfile().getName() + " " + (hasPerm ? "has" : "does not have") + " the permission \"" + permission + "\""), false);
        return 0;
    }
    public static int setPerm(CommandContext<ServerCommandSource> context, boolean enabled) throws CommandSyntaxException {
        for (ServerPlayerEntity player : EntityArgumentType.getPlayers(context, "players")) {
            String permission = StringArgumentType.getString(context,"permission");
            boolean hasPerm = Thimble.PERMISSIONS.hasPermission(permission, player.getGameProfile().getId());
            if (hasPerm == enabled) {
                context.getSource().sendFeedback(new LiteralText(player.getGameProfile().getName() + " " + (enabled ? "already has" : "never had") + " the permission \"" + permission + "\"").formatted(Formatting.RED), false);
            } else {
                context.getSource().sendFeedback(new LiteralText(player.getGameProfile().getName() + " " + (enabled ? "has been granted" : "no longer has") + " the permission \"" + permission + "\"").formatted(Formatting.GREEN), false);
                context.getSource().getMinecraftServer().sendMessage(new LiteralText( "").append(context.getSource().getDisplayName()).append(new LiteralText(" has " + (enabled ? "granted" : "revoked") + " the permission \"" + permission + "\" for player " + player.getGameProfile().getName())));
                if (!context.getSource().getName().equals(player.getGameProfile().getName())) player.sendMessage(new LiteralText(context.getSource().getName() + " " + (enabled ? "has given you the" : "has taken away your") + " \"" + permission + "\" permission.").formatted(enabled ? Formatting.GREEN : Formatting.RED));
                if (enabled) Thimble.PERMISSIONS.getPlayer(player.getGameProfile().getId()).permission(permission);
                else Thimble.PERMISSIONS.getPlayer(player.getGameProfile().getId()).removePermission(permission);
                //permission.onStateChanged(player, enabled);
            }
        }
        return 0;
    }
    public static ArgumentBuilder permissionArgumentBuilder(String name) {
        RequiredArgumentBuilder<ServerCommandSource, String> builder = RequiredArgumentBuilder.argument(name, StringArgumentType.word());
        builder.suggests(SUGGESTIONS_BUILDER);
        return builder;
    }
}
