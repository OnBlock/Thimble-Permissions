package io.github.indicode.fabric.permissions.mixin;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.impl.SyntaxError;
import com.google.gson.JsonElement;
import io.github.cottonmc.jankson.JanksonFactory;
import io.github.indicode.fabric.permissions.PermissionMap;
import io.github.indicode.fabric.permissions.Thimble;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Pair;
import net.minecraft.world.level.LevelGeneratorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

/**
 * @author Indigo Amann
 */
@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
    @Inject(method = "loadWorld", at = @At("HEAD"))
    protected void loadPerms(String string_1, String string_2, long long_1, LevelGeneratorType levelGeneratorType_1, JsonElement jsonElement_1, CallbackInfo ci) {
        Thimble.PERMISSIONS = new PermissionMap();
        //Thimble.registerDispatcherCommands(((MinecraftServer)(Object)this).getCommandManager().getDispatcher());
        Thimble.permissionWriters.forEach(consumer -> consumer.accept(new Pair<>(Thimble.PERMISSIONS, ((MinecraftServer)(Object)this))));
        try {
            Jankson jankson = JanksonFactory.createJankson();
            if (!Thimble.PERMS_FILE.exists()) Thimble.PERMS_FILE.createNewFile();
            else Thimble.PERMISSIONS.permissionsFromJson(DefaultedJsonObject.of(jankson.load(Thimble.PERMS_FILE)));
            if (!Thimble.PERMS_DATA_FILE.exists()) Thimble.PERMS_DATA_FILE.createNewFile();
            else Thimble.PERMISSIONS.playersFromJson(DefaultedJsonObject.of(jankson.load(Thimble.PERMS_DATA_FILE)));

        } catch (SyntaxError | IOException syntaxError) {
            throw new RuntimeException(syntaxError);
        }
    }
}
