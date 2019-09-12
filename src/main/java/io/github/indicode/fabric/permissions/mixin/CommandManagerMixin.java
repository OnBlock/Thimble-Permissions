package io.github.indicode.fabric.permissions.mixin;

import com.mojang.brigadier.CommandDispatcher;
import io.github.indicode.fabric.permissions.Permission;
import io.github.indicode.fabric.permissions.command.PermissionCommand;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Indigo Amann
 */
@Mixin(CommandManager.class)
public class CommandManagerMixin {
    @Shadow
    private CommandDispatcher<ServerCommandSource> dispatcher;
    @Inject(method = "<init>", at = @At("RETURN"))
    public void addCommand(boolean bool, CallbackInfo ci) {
        PermissionCommand.register(dispatcher);
    }
}
