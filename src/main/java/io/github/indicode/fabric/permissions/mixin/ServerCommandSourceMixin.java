package io.github.indicode.fabric.permissions.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.indicode.fabric.permissions.Thimble;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import org.lwjgl.system.CallbackI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Indigo Amann
 */
@Mixin(ServerCommandSource.class)
public class ServerCommandSourceMixin {
    @Inject(method = "hasPermissionLevel", at = @At("RETURN"), cancellable = true)
    public void hasPermissionLevel(int int_1, CallbackInfoReturnable ci) {
        ServerCommandSource source = (ServerCommandSource)(Object)this;
        try {
            if ((source.getEntity() != null && source.getEntity() instanceof PlayerEntity && Thimble.PERMISSIONS.hasPermission(Thimble.getCommandPermission(source.getName()), source.getPlayer().getGameProfile().getId()))) ci.setReturnValue(true);
        } catch (CommandSyntaxException e) {
            e.printStackTrace(); // How did this even happen?
        }
    }
}
