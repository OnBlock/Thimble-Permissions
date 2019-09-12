package io.github.indicode.fabric.permissions.mixin;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.impl.SyntaxError;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.datafixers.DataFixer;
import io.github.cottonmc.jankson.JanksonFactory;
import io.github.indicode.fabric.permissions.Permission;
import io.github.indicode.fabric.permissions.PermissionMap;
import io.github.indicode.fabric.permissions.Thimble;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonObject;
import io.github.indicode.fabric.tinyconfig.JanksonLoader;
import jdk.jfr.events.ThrowablesEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.util.UserCache;
import net.minecraft.world.level.LevelGeneratorType;
import org.lwjgl.system.CallbackI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.Proxy;

/**
 * @author Indigo Amann
 */
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "loadWorld", at = @At("HEAD"))
    protected void loadPerms(String string_1, String string_2, long long_1, LevelGeneratorType levelGeneratorType_1, JsonElement jsonElement_1, CallbackInfo ci) {
        Thimble.PERMISSIONS = new PermissionMap();
        try {
            Jankson jankson = JanksonFactory.createJankson();
            if (!Thimble.PERMS_FILE.exists()) Thimble.PERMS_FILE.createNewFile();
            else Thimble.PERMISSIONS.permissionsFromJson(DefaultedJsonObject.of(jankson.load(Thimble.PERMS_FILE)));
            if (!Thimble.PERMS_DATA_FILE.exists()) Thimble.PERMS_DATA_FILE.createNewFile();
            else Thimble.PERMISSIONS.playersFromJson(DefaultedJsonObject.of(jankson.load(Thimble.PERMS_DATA_FILE)));

        } catch (SyntaxError | IOException syntaxError) {
            throw new RuntimeException(syntaxError);
        }
        Thimble.registerDispatcherCommands(((MinecraftServer)(Object)this).getCommandManager().getDispatcher());
        System.out.println("SERVload");
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
