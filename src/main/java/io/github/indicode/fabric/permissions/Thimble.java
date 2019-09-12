package io.github.indicode.fabric.permissions;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import io.github.indicode.fabric.permissions.command.CommandPermission;
import io.github.indicode.fabric.permissions.command.PermissionArgument;
import io.github.indicode.fabric.permissions.command.PermissionCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.fabricmc.loader.FabricLoader;
import net.minecraft.command.arguments.ArgumentTypes;
import net.minecraft.command.arguments.serialize.ArgumentSerializer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.PacketByteBuf;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Indigo Amann
 */
public class Thimble implements ModInitializer {
    public static final File PERMS_FILE = new File(FabricLoader.INSTANCE.getGameDirectory().getPath() + "/permissions.json");
    public static final File PERMS_DATA_FILE = new File(FabricLoader.INSTANCE.getGameDirectory().getPath() + "/permissions_dat.json");
    public static PermissionMap PERMISSIONS = new PermissionMap();
    private static final List<Getter<String, Permission>> permissionReaders = new ArrayList<>();
    private static final Map<String, Permission> COMMAND_PERMISSIONS = new HashMap<>();
    public static final String COMMANDS = "minecraft.command";

    @Override
    public void onInitialize() {
        CommandRegistry.INSTANCE.register(false, PermissionCommand::register);
    }
    public static void onPermissionsRead(Getter<String, Permission> permissionReader) {
        permissionReaders.add(permissionReader);
    }
    public static Permission getCommandPermission(String command) {
        if (COMMAND_PERMISSIONS.containsKey(command)) return COMMAND_PERMISSIONS.get(command);
        else {
            CommandPermission permission = new CommandPermission(command);
            COMMAND_PERMISSIONS.put(command, permission);
            return permission;
        }
    }
    public static void registerDispatcherCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        for (CommandNode<ServerCommandSource> child : dispatcher.getRoot().getChildren()) {
            Permission permission = Thimble.getCommandPermission(child.getName());
            Class c = CommandNode.class;
            try {
                Field requirement = c.getDeclaredField("requirement");
                requirement.setAccessible(true);
                requirement.set(child, child.getRequirement().or(source -> {
                    try {
                        return (source.getEntity() != null && source.getEntity() instanceof PlayerEntity && Thimble.PERMISSIONS.hasPermission(permission, source.getPlayer().getGameProfile().getId()));
                    } catch (CommandSyntaxException e) {
                        // ignore
                    }
                    return false;
                }));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace(); // Should never happen
            }
        }
    }
}
