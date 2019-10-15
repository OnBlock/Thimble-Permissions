package io.github.indicode.fabric.permissions;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonPrimitive;
import io.github.indicode.fabric.tinyconfig.ModConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Config {
    private static final ModConfig MOD_CONFIG = new ModConfig("thimble");
    public static HashMap<String, List<String>> ADMIN_GROUPS = new HashMap<>();
    public static void sync(boolean overwrite) {
        MOD_CONFIG.configure(overwrite, config -> {
            ADMIN_GROUPS.clear();
            config.accessChild("admin_groups", groups -> {
                groups.forEach((id, element) -> {
                    if (element instanceof JsonArray) {
                        JsonArray array = (JsonArray) element;
                        List<String> permission = new ArrayList<>();
                        array.forEach(subelement -> {
                            if (subelement instanceof JsonPrimitive) permission.add(((JsonPrimitive) subelement).asString());
                        });
                        ADMIN_GROUPS.put(id, permission);
                    }
                });
            }, "Players with these permissions will be able to change listed permissions for other players");
        });
    }
}
