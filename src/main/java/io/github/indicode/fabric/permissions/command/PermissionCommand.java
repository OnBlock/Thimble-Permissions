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
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.indicode.fabric.permissions.Permission;
import io.github.indicode.fabric.permissions.Thimble;
import net.minecraft.command.CommandException;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.Map;


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
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal("permission").requires(source -> source.hasPermissionLevel(4));
        {
            LiteralArgumentBuilder<ServerCommandSource> check = CommandManager.literal("check");
            ArgumentBuilder player = CommandManager.argument("player", EntityArgumentType.player());
            ArgumentBuilder permission = permissionArgumentBuilder("permission");
            permission.executes(PermissionCommand::checkPerm);
            player.then(permission);
            check.then(player);
            builder.then(check);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> set = CommandManager.literal("set");
            LiteralArgumentBuilder<ServerCommandSource> grant = CommandManager.literal("grant");
            LiteralArgumentBuilder<ServerCommandSource> revoke = CommandManager.literal("revoke");
            ArgumentBuilder player = CommandManager.argument("player", EntityArgumentType.player());
            ArgumentBuilder permission = permissionArgumentBuilder("permission");
            ArgumentBuilder playerGrant = CommandManager.argument("player", EntityArgumentType.player());
            ArgumentBuilder permissionGrant = permissionArgumentBuilder("permission");
            permissionGrant.executes(context -> setPerm(context, true));
            ArgumentBuilder playerRevoke = CommandManager.argument("player", EntityArgumentType.player());
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
        LiteralCommandNode node = dispatcher.register(builder);
    }
    public static int checkPerm(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        Permission permission = getPermission(StringArgumentType.getString(context,"permission"));
        boolean hasPerm = Thimble.PERMISSIONS.hasPermission(permission, player.getGameProfile().getId());
        context.getSource().sendFeedback(new LiteralText(player.getGameProfile().getName() + " " + (hasPerm ? "has" : "does not have") + " the permission \"" + permission.getFullIdentifier() + "\""), false);
        return 0;
    }
    public static int setPerm(CommandContext<ServerCommandSource> context, boolean enabled) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        Permission permission = getPermission(StringArgumentType.getString(context,"permission"));
        boolean hasPerm = Thimble.PERMISSIONS.hasPermission(permission, player.getGameProfile().getId());
        if (hasPerm == enabled) {
            context.getSource().sendFeedback(new LiteralText(player.getGameProfile().getName() + " " + (enabled ? "already has" : "never had") + " the permission \"" + permission.getFullIdentifier() + "\"").formatted(Formatting.RED), false);
        } else {
            context.getSource().sendFeedback(new LiteralText(player.getGameProfile().getName() + " " + (enabled ? "has been granted" : "no longer has") + " the permission \"" + permission.getFullIdentifier() + "\"").formatted(Formatting.GREEN), false);
            context.getSource().getMinecraftServer().sendMessage(new LiteralText( "").append(context.getSource().getDisplayName()).append(new LiteralText(" has " + (enabled ? "granted" : "revoked") + " the permission \"" + permission.getFullIdentifier() + "\" for player " + player.getGameProfile().getName())));
            if (!context.getSource().getName().equals(player.getGameProfile().getName())) player.sendMessage(new LiteralText(context.getSource().getName() + " " + (enabled ? "has given you the" : "has taken away your") + " \"" + permission.getFullIdentifier() + "\" permission.").formatted(enabled ? Formatting.GREEN : Formatting.RED));
            if (enabled) Thimble.PERMISSIONS.getPlayer(player.getGameProfile().getId()).permission(permission);
            else Thimble.PERMISSIONS.getPlayer(player.getGameProfile().getId()).removePermission(permission);
            permission.onStateChanged(player, enabled);
        }




        return 0;
    }
    public static ArgumentBuilder permissionArgumentBuilder(String name) {
        RequiredArgumentBuilder<ServerCommandSource, String> builder = RequiredArgumentBuilder.argument(name, StringArgumentType.word());
        builder.suggests(SUGGESTIONS_BUILDER);
        return builder;
    }
    public static Permission getPermission(String perm) throws CommandException {
        Map<String, Permission> permissionMap = Thimble.PERMISSIONS.mapPermissions(Thimble.PERMISSIONS.getRegisteredPermissions());
        if (!permissionMap.containsKey(perm)) throw new CommandException(new LiteralText("\"" + perm + "\" is not a permission.").formatted(Formatting.RED));
        else return permissionMap.get(perm);
    }
}
