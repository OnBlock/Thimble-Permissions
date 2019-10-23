package io.github.indicode.fabric.permissions;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.impl.SyntaxError;
import io.github.cottonmc.jankson.JanksonFactory;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonObject;
import io.github.indicode.fabric.worlddata.NBTWorldData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Pair;

import java.io.File;
import java.io.IOException;

public class PermissionLoadHandler extends NBTWorldData {
    public static MinecraftServer server;

    @Override
    public File getSaveFile(File worldDir, File runDir, boolean backup) {
        return new File(worldDir, "permissions." + (backup ? "dat_old" : "dat"));
    }

    @Override
    public void onWorldLoad(File file, File file1) {
        File perms = new File(file, "permissions.json");
        Thimble.PERMISSIONS = new PermissionMap();
        Thimble.LOGGER.debug("Running mod permission injectors");
        Thimble.permissionWriters.forEach(consumer -> consumer.accept(new Pair<>(Thimble.PERMISSIONS, server)));
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
        super.onWorldLoad(file, file1);
    }

    @Override
    public CompoundTag toNBT(CompoundTag compoundTag) {
        return Thimble.PERMISSIONS.writePlayersToTag(compoundTag);
    }

    @Override
    public void fromNBT(CompoundTag compoundTag) {
        Thimble.PERMISSIONS.readPlayersFromTag(compoundTag);
    }
}
