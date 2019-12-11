package io.github.indicode.fabric.permissions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import io.github.indicode.fabric.worlddata.WorldDataLib;
import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.SimpleMessage;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author Indigo Amann
 */
public class Thimble implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("Thimble", new MessageFactory() {
        @Override
        public Message newMessage(Object message) {
            return new SimpleMessage("[Thimble] " + message);
        }

        @Override
        public Message newMessage(String message) {
            return new SimpleMessage("[Thimble] " + message);
        }

        @Override
        public Message newMessage(String message, Object... params) {
            return new SimpleMessage("[Thimble] " + message);
        }
    });
    public static PermissionMap PERMISSIONS = new PermissionMap();
    public static final List<BiConsumer<PermissionMap, MinecraftServer>> permissionWriters = new ArrayList<>();
    private static boolean vanillaDispatcherDisabled = false;
    public static void disableVanillaCommandPerms() {
        vanillaDispatcherDisabled = true;
    }
    private static PermissionLoadHandler loadHandler = new PermissionLoadHandler();
    public static void reload() {
        WorldDataLib.triggerCallbackLoad(loadHandler);
    }

    @Override
    public void onInitialize() {
        WorldDataLib.addIOCallback(loadHandler);
        Config.sync(false);
        permissionWriters.add((map, server) -> {
            Thimble.registerCommandPermission("thimble.check");
            Thimble.registerCommandPermission("thimble.modify");
            Thimble.registerCommandPermission("thimble.reload");
            if (!vanillaDispatcherDisabled) registerDispatcherCommands("minecraft.command", server.getCommandManager().getDispatcher());
        });
    }
    public static void registerCommandPermission(String permission) {
        PERMISSIONS.registerPermission(permission.replace(":", "_"), PermChangeBehavior.UPDATE_COMMAND_TREE);
    }
    public static void registerDispatcherCommands(String prefix, CommandDispatcher<ServerCommandSource> dispatcher) {
        for (CommandNode<ServerCommandSource> child : dispatcher.getRoot().getChildren()) {
            String permission = prefix + "." + child.getName();
            registerCommandPermission(permission);
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
}
