package io.github.indicode.fabric.permissions.command;

import io.github.indicode.fabric.permissions.Permission;
import io.github.indicode.fabric.permissions.Thimble;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Indigo Amann
 */
public class CommandPermission extends Permission {
    public CommandPermission(String command) {
        super(command, Thimble.PERMISSIONS.getPermission(Thimble.COMMANDS));
    }
    @Override
    public boolean shouldSave() {
        System.out.println("Dont save!");
        return false;
    }
    @Override
    public void onStateChanged(ServerPlayerEntity player, boolean hasPerm) {
        System.out.println("updating command perm");
        player.server.getCommandManager().sendCommandTree(player);
    }
}
