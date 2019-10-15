package io.github.indicode.fabric.permissions;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import io.github.indicode.fabric.permissions.command.CommandPermission;
import io.github.indicode.fabric.permissions.command.PermissionCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.FabricLoader;
import net.minecraft.command.arguments.ArgumentTypes;
import net.minecraft.command.arguments.serialize.ArgumentSerializer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.Pair;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
    public static final List<Consumer<Pair<PermissionMap, MinecraftServer>>> permissionWriters = new ArrayList<>();
    private static final Map<String, Permission> COMMAND_PERMISSIONS = new HashMap<>();
    public static final String COMMANDS = "minecraft.command";
    public static final List<String> disabledCommandPerms = new ArrayList<>();
    private static boolean vanillaDispatcherDisabled = false;
    public static void disableVanillaDispatcherPerms() {
        vanillaDispatcherDisabled = true;
    }

    @Override
    public void onInitialize() {
        Config.sync(false);
        permissionWriters.add(pair -> {
            try {
                pair.getLeft().getPermission("minecraft", CommandPermission.class);
                pair.getLeft().getPermission(COMMANDS, CommandPermission.class);
                pair.getLeft().getPermission("thimble", CommandPermission.class);
                pair.getLeft().getPermission("thimble.check", CommandPermission.class);
                pair.getLeft().getPermission("thimble.modify", CommandPermission.class);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                e.printStackTrace();
            }
            if (!vanillaDispatcherDisabled)registerDispatcherCommands(COMMANDS, pair.getRight().getCommandManager().getDispatcher());

        });
    }
    public static Permission getCommandPermission(String prefix, String command) {
        command = command.replace(":", "_"); // This is added because *SOMEBODY* decided to add this stupid, command-incompatable character into command names >:(
        if (COMMAND_PERMISSIONS.containsKey(command)) return COMMAND_PERMISSIONS.get(command);
        else {
            CommandPermission permission = new CommandPermission(command, PERMISSIONS.getPermission(prefix));
            COMMAND_PERMISSIONS.put(command, permission);
            return permission;
        }
    }
    public static void registerDispatcherCommands(String prefix, CommandDispatcher<ServerCommandSource> dispatcher) {
        for (CommandNode<ServerCommandSource> child : dispatcher.getRoot().getChildren()) {
            if (disabledCommandPerms.contains(child)) continue;
            Permission permission = Thimble.getCommandPermission(prefix, child.getName());
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
    public static boolean hasPermissionOrOp(ServerCommandSource source, String permission, int opLevel) {
        if(source.hasPermissionLevel(opLevel)) return true;
        try {
            return Thimble.PERMISSIONS.hasPermission(permission, source.getPlayer().getGameProfile().getId());
        } catch (CommandSyntaxException e) {
            return false;
        }
    }
    public static boolean hasPermissionChildOrOp(ServerCommandSource source, String permission, int opLevel) {
        if(source.hasPermissionLevel(opLevel)) return true;
        try {
            return Thimble.PERMISSIONS.hasPermissionOrChild(permission, source.getPlayer().getGameProfile().getId());
        } catch (CommandSyntaxException e) {
            return false;
        }
    }
    /*
    public static List<String> writePermTree(List<String> perms) {

    }
    public static Map<String, Object> mapPermissionsByParent(List<String> permissions) {
        Map<String, Object> map = new HashMap<>();
        Map<String, >
        for(String perm: permissions) {
            String[] nameSplit = perm.split("[.]");
            String lastPerm = null;
            int i;
            for (i = nameSplit.length - 1; i >= 0; i--) {
                String nameSlice = "";
                for (int j = 0; j < i; j++) {
                    nameSlice += (j == 0 ? "" : ".") + nameSplit[j];
                }
                if (map.containsKey(nameSlice)) {
                    lastPerm = nameSlice;
                    break;
                }
            }
            Permission current;
            if (lastPerm == null) {
                current = new Permission(nameSplit[0]);
                addGroup(current);
                i = 1;
            } else current = map.get(lastPerm);
            while (i < nameSplit.length) {
                current = new Permission(nameSplit[i], current);
                i++;
            }
        }
    }

     */
}
