package io.github.indicode.fabric.permissions.mixin;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.impl.SyntaxError;
import com.google.gson.JsonElement;
import io.github.cottonmc.jankson.JanksonFactory;
import io.github.indicode.fabric.permissions.PermissionMap;
import io.github.indicode.fabric.permissions.Thimble;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Pair;
import net.minecraft.world.level.LevelGeneratorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Indigo Amann
 */
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
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
    @Inject(method = "save", at = @At("HEAD"))
    public void savePerms(boolean b1, boolean b2, boolean b3, CallbackInfoReturnable ci) {
        try {
            if (!Thimble.PERMS_FILE.exists()) Thimble.PERMS_FILE.createNewFile();
            if (!Thimble.PERMS_DATA_FILE.exists()) Thimble.PERMS_DATA_FILE.createNewFile();
            String perms = Thimble.PERMISSIONS.permissionsToJson().toJson(JsonGrammar.JANKSON);
            String players = Thimble.PERMISSIONS.playersToJson().toJson(JsonGrammar.builder().printWhitespace(false).build());

            PrintWriter pout = new PrintWriter(Thimble.PERMS_FILE);
            PrintWriter ppout = new PrintWriter(Thimble.PERMS_DATA_FILE);
            Throwable var4 = null;

            try {
                pout.print(perms);
                ppout.print(players);
            } catch (Throwable var13) {
                var4 = var13;
                throw var13;
            } finally {
                if (pout != null) {
                    if (var4 != null) {
                        try {
                            pout.close();
                        } catch (Throwable var12) {
                            var4.addSuppressed(var12);
                        }
                    } else {
                        pout.close();
                    }
                }
                if (ppout != null) {
                    if (var4 != null) {
                        try {
                            ppout.close();
                        } catch (Throwable var12) {
                            var4.addSuppressed(var12);
                        }
                    } else {
                        ppout.close();
                    }
                }
            }
        } catch (IOException syntaxError) {
            throw new RuntimeException(syntaxError);
        }
    }
}
