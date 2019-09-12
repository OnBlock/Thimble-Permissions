package io.github.indicode.fabric.permissions.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.indicode.fabric.permissions.Permission;
import io.github.indicode.fabric.permissions.Thimble;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import javax.naming.event.EventDirContext;
import javax.xml.transform.Source;

/**
 * @author Indigo Amann
 */
public class PermissionCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal("permission").requires(source -> source.hasPermissionLevel(4));
        {
            LiteralArgumentBuilder<ServerCommandSource> check = CommandManager.literal("check");
            ArgumentBuilder player = CommandManager.argument("player", EntityArgumentType.player());
            ArgumentBuilder permission = CommandManager.argument("permission", new PermissionArgument(() -> Thimble.PERMISSIONS));
            permission.executes(PermissionCommand::checkPerm);
            player.then(permission);
            check.then(player);
            builder.then(check);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> set = CommandManager.literal("set");
            ArgumentBuilder player = CommandManager.argument("player", EntityArgumentType.player());
            ArgumentBuilder permission = CommandManager.argument("permission", new PermissionArgument(() -> Thimble.PERMISSIONS));
            ArgumentBuilder enabled = CommandManager.argument("enabled", BoolArgumentType.bool());
            enabled.executes(PermissionCommand::setPerm);
            permission.then(enabled);
            player.then(permission);
            set.then(player);
            builder.then(set);
        }
        LiteralCommandNode node = dispatcher.register(builder);
    }
    public static int checkPerm(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        Permission permission = context.getArgument("permission", Permission.class);
        boolean hasPerm = Thimble.PERMISSIONS.hasPermission(permission, player.getGameProfile().getId());
        context.getSource().sendFeedback(new LiteralText(player.getGameProfile().getName() + " " + (hasPerm ? "has" : "does not have") + " the permission \"" + permission.getFullIdentifier() + "\""), false);
        return 0;
    }
    public static int setPerm(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        Permission permission = context.getArgument("permission", Permission.class);
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        boolean hasPerm = Thimble.PERMISSIONS.hasPermission(permission, player.getGameProfile().getId());
        if (hasPerm == enabled) {
            context.getSource().sendFeedback(new LiteralText(player.getGameProfile().getName() + " " + (enabled ? "already has" : "never had") + " the permission \"" + permission.getFullIdentifier() + "\"").formatted(Formatting.RED), false);
        } else {
            context.getSource().sendFeedback(new LiteralText(player.getGameProfile().getName() + " " + (enabled ? "has been granted" : "no longer has") + " the permission \"" + permission.getFullIdentifier() + "\"").formatted(Formatting.GREEN), false);
            context.getSource().getMinecraftServer().sendMessage(new LiteralText( "").append(context.getSource().getDisplayName()).append(new LiteralText(" has " + (enabled ? "granted" : "revoked") + " the permission \"" + permission.getFullIdentifier() + "\" for player " + player.getGameProfile().getName())));
            if (!context.getSource().getName().equals(player.getGameProfile().getName())) player.sendMessage(new LiteralText(context.getSource().getName() + " " + (enabled ? "has given you the" : "has taken away your") + " \"" + permission.getFullIdentifier() + "\" permission.").formatted(enabled ? Formatting.GREEN : Formatting.RED));
        }
        if (enabled) Thimble.PERMISSIONS.getPlayer(player.getGameProfile().getId()).permission(permission);
        else Thimble.PERMISSIONS.getPlayer(player.getGameProfile().getId()).removePermission(permission);

        player.server.getCommandManager().sendCommandTree(player);

        return 0;
    }
}
