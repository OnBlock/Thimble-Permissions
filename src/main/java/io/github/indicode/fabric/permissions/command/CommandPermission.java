package io.github.indicode.fabric.permissions.command;

import io.github.indicode.fabric.permissions.Permission;
import io.github.indicode.fabric.permissions.Thimble;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Indigo Amann
 */
public class CommandPermission extends Permission {
    public CommandPermission(String command, Permission parent) {
        super(command, parent);
    }
    public CommandPermission(String command) {
        super(command, Thimble.PERMISSIONS.getPermission(Thimble.COMMANDS));
    }
    @Override
    public boolean shouldSave() {
        return false;
    }
    @Override
    public void onStateChanged(ServerPlayerEntity player, boolean hasPerm) {
        player.server.getCommandManager().sendCommandTree(player);
    }
}
