package io.github.indicode.fabric.permissions;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.FabricLoader;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Indigo Amann
 */
public class Thimble implements ModInitializer {
    public static PermissionMap PERMISSIONS = new PermissionMap();
    private static final List<Getter<String, Permission>> permissionReaders = new ArrayList<>();
    private static final Map<String, Permission> COMMAND_PERMISSIONS = new HashMap<>();
    public static final Permission MINECRAFT = new Permission("minecraft");
    public static final Permission COMMANDS = new Permission("command", MINECRAFT);

    @Override
    public void onInitialize() {

    }
    public static void onPermissionsRead(Getter<String, Permission> permissionReader) {
        permissionReaders.add(permissionReader);
    }
    public static Permission getCommandPermission(String command) {
        if (COMMAND_PERMISSIONS.containsKey(command)) return COMMAND_PERMISSIONS.get(command);
        else {
            Permission permission = new Permission(command, COMMANDS);
            COMMAND_PERMISSIONS.put(command, permission);
            return permission;
        }
    }
}
