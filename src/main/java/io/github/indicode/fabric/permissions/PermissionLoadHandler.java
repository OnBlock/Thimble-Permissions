package io.github.indicode.fabric.permissions;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.impl.SyntaxError;
import io.github.cottonmc.jankson.JanksonFactory;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonObject;
import io.github.indicode.fabric.worlddata.NBTWorldData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class PermissionLoadHandler extends NBTWorldData {
    public static MinecraftServer server;
    public static boolean reloading = false;

    @Override
    public File getSaveFile(File worldDir, File runDir, boolean backup) {
        return new File(worldDir, "permissions." + (backup ? "dat_old" : "dat"));
    }

    @Override
    public void onWorldLoad(File file, File file1) {
        try {
            File perms = new File(file, "permissions.json");
            Map<UUID, PlayerPermissionManager> oldmap = Thimble.PERMISSIONS.permissionMap;
            Thimble.PERMISSIONS = new PermissionMap();
            Thimble.LOGGER.debug("Running mod permission injectors");
            Thimble.permissionWriters.forEach(consumer -> consumer.accept(Thimble.PERMISSIONS, server));
            Thimble.LOGGER.debug("Loading permissions json");
            try {
                Jankson jankson = JanksonFactory.createJankson();
                if (!perms.exists()) {
                    Thimble.LOGGER.warn("permissions.json does not exist");
                } else {
                    Thimble.PERMISSIONS.permissionsFromJson(DefaultedJsonObject.of(jankson.load(perms)));
                }

            } catch (SyntaxError | IOException syntaxError) {
                Thimble.LOGGER.error("Could not load permissions file", syntaxError);
                System.exit(1);
            }
            Thimble.LOGGER.debug("Loading player permissions");
            if (reloading) {
                Thimble.PERMISSIONS.permissionMap = oldmap;
            } else {
                super.onWorldLoad(file, file1);
            }
            reloading = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public CompoundTag toNBT(CompoundTag compoundTag) {
        return Thimble.PERMISSIONS.writePlayersToTag(compoundTag);
    }

    @Override
    public void fromNBT(CompoundTag compoundTag) {
        try {
            Thimble.PERMISSIONS.readPlayersFromTag(compoundTag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
