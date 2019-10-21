package io.github.indicode.fabric.permissions;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.impl.SyntaxError;
import io.github.cottonmc.jankson.JanksonFactory;
import io.github.indicode.fabric.permissions.mixin.MinecraftServerMixin;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonObject;
import io.github.indicode.fabric.worlddata.WorldIOCalback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Pair;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class PermissionLoadHandler implements WorldIOCalback {
    public static MinecraftServer server;
    @Override
    public void onWorldSave(File file, File file1) {
        File dat = new File(file, "permissions_dat.json");
        String playersJson = Thimble.PERMISSIONS.playersToJson().toJson(JsonGrammar.builder().printWhitespace(false).build());
        if (playersJson != null) {
            try {
                if (!dat.exists()) dat.createNewFile();
                PrintWriter ppout = new PrintWriter(dat);
                Throwable var4 = null;

                try {
                    ppout.print(playersJson);
                } catch (Throwable var13) {
                    var4 = var13;
                    throw var13;
                } finally {
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
            } catch (IOException error) {
                Thimble.LOGGER.error("There was a problem saving the player data json", error);
            }
        } else {
            Thimble.LOGGER.error("Permissions data is null. You should never see this message.");
        }
    }

    @Override
    public void onWorldLoad(File file, File file1) {
        File perms = new File(file, "permissions.json");
        File dat = new File(file, "permissions_dat.json");
        Thimble.PERMISSIONS = new PermissionMap();
        Thimble.LOGGER.debug("Loading permissions json");
        try {
            Jankson jankson = JanksonFactory.createJankson();
            if (!perms.exists()) {
                Thimble.LOGGER.warn("permissions.json does not exist");
            } else {
                Thimble.PERMISSIONS.permissionsFromJson(DefaultedJsonObject.of(jankson.load(perms)));
            }
            if (!dat.exists()) {
                Thimble.LOGGER.warn("permissions_dat.json does not exist");
            } else {
                Thimble.PERMISSIONS.playersFromJson(DefaultedJsonObject.of(jankson.load(dat)));
            }

        } catch (SyntaxError | IOException syntaxError) {
            Thimble.LOGGER.error("Could not load permissions file", syntaxError);
            System.exit(1);
        }
        Thimble.LOGGER.debug("Loaded permissions from files");
        Thimble.permissionWriters.forEach(consumer -> consumer.accept(new Pair<>(Thimble.PERMISSIONS, server)));
        Thimble.LOGGER.debug("Ran mod permission injectors");
    }
}
