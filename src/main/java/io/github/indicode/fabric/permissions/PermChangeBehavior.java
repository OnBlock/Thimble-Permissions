package io.github.indicode.fabric.permissions;

import net.minecraft.server.network.ServerPlayerEntity;

public interface PermChangeBehavior {
    static final PermChangeBehavior UPDATE_COMMAND_TREE = (newState, target) -> target.server.getCommandManager().sendCommandTree(target);
    void onStateChange(boolean newState, ServerPlayerEntity target);
}
