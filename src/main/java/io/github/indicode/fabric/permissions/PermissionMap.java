package io.github.indicode.fabric.permissions;

import blue.endless.jankson.*;
import com.google.common.collect.ImmutableList;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * @author Indigo Amann
 */
public class PermissionMap {
    protected Map<UUID, PlayerPermissionManager> permissionMap = new HashMap<>();
    protected Map<String, Permission> permissions = new HashMap();
    protected String defaultPermission = null;
    public boolean defaultPermissionMatches(String permission) {
        return defaultPermission != null && defaultPermission.equals(permission);
    }
    public ImmutableList<String> getRegisteredPermissions() {
        return ImmutableList.copyOf(permissions.keySet());
    }
    public PlayerPermissionManager getPlayer(UUID uuid) {
        if (permissionMap.containsKey(uuid)) return permissionMap.get(uuid);
        PlayerPermissionManager manager = new PlayerPermissionManager(this);
        permissionMap.put(uuid, manager);
        return manager;
    }
    public void registerPermission(String permission, PermChangeBehavior... behaviors) {
        if (permission == null || permission.isEmpty()) {
            throw new IllegalArgumentException("Permission cannot be null or empty.");
        }
        if (permissionExists(permission)) {
            throw new IllegalStateException(String.format("Permission \"%s\" is already defined."));
        }
        permissions.put(permission, new Permission(behaviors));
        String removeTop = permission.substring(0, permission.lastIndexOf(permission.split("[.]")[permission.split("[.]").length - 1]));
        if (!removeTop.isEmpty() && !permissionExists(removeTop)) {
            registerPermission(removeTop);
        }
    }
    public void updatePermissionStateHandlers(String permission, ServerPlayerEntity target) {
        Permission data = getPermissionData(permission);
        for (PermChangeBehavior behavior : getPermissionData(permission).behaviors) {
            behavior.onStateChange(hasPermission(permission, target.getGameProfile().getId()), target);
        }
        for (String inherit : data.inheritance) {
            updatePermissionStateHandlers(inherit, target);
        }
    }
    public boolean permissionExists(String permission) {
        return permission != null && !permission.isEmpty() && permissions.containsKey(permission);
    }
    public boolean isChildOrSame(String parent, String child) {
        return child != null && parent != null && child.startsWith(parent);
    }
    public Permission getPermissionData(String permission) {
        return permissions.get(permission);
    }
    public boolean isInherited(String permission, String inherited) {
        if (permission == null) return false;
        Permission permission_ = getPermissionData(permission);
        if (permission_ == null) return false;
        if (isChildOrSame(permission, inherited)) return true;
        for (String inherit : permission_.inheritance) {
            if (isChildOrSame(inherit, inherited))  return true;
            if (isInherited(inherit, inherited))  return true;
        }
        return false;
    }
    public boolean hasPermission(String permission, UUID player) {
        return getPlayer(player).hasPermission(permission);
    }
    /*public DefaultedJsonObject toJson() {
        DefaultedJsonObject jsonObject = new DefaultedJsonObject();
        jsonObject.set("permissions", permissionsToJson());
        jsonObject.set("players", playersToJson());
        return jsonObject;
    }*/
    /*public DefaultedJsonObject permissionsToJson() {
        DefaultedJsonObject jsonObject = new DefaultedJsonObject();
        for (String permission : permissions.keySet()) {
            if (permission.shouldSave) jsonObject.set(permission.identifier, permission.toJson());
        }
        return jsonObject;
    }*/
    /*@Deprecated
    public DefaultedJsonObject playersToJson() {
        DefaultedJsonObject jsonObject = new DefaultedJsonObject();
        permissionMap.forEach((uuid, manager) -> {
            jsonObject.set(uuid.toString(), manager.toJson());
        });
        return jsonObject;
    }*/
    public CompoundTag writePlayersToTag(CompoundTag tag) {
        // did not do it in the more compact array per permission form to account for future integer perms and force-denied perms
        permissionMap.forEach((uuid, manager) -> tag.put(uuid.toString(), manager.toNBT()));
        return tag;
    }
    protected Map<String, DefaultedJsonObject> loadBlankPermissionTree(DefaultedJsonObject tree, Map<String, DefaultedJsonObject> existingPermissions, String nestLevel) {
        Map<String, DefaultedJsonObject> keyMap = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : tree.entrySet()) {
            String id = nestLevel == null ? entry.getKey() : nestLevel + "." + entry.getKey();
            if (id.equals("*")) continue;
            if (existingPermissions.containsKey(id) || permissionExists(id)) continue;
            if (entry.getValue() instanceof JsonObject) {
                keyMap.put(id, DefaultedJsonObject.of((JsonObject) entry.getValue()));
                keyMap.putAll(loadBlankPermissionTree(DefaultedJsonObject.of((JsonObject) entry.getValue()), keyMap, id));
            } else if (entry.getValue() == null || entry.getValue().equals(JsonNull.INSTANCE)) {
                keyMap.put(id, null);
            }
        }
        return keyMap;
    }
    public void permissionsFromJson(DefaultedJsonObject tree) {
        List<String> existingPermissions = getRegisteredPermissions();
        Map<String, DefaultedJsonObject> permissionMap = loadBlankPermissionTree(tree, new HashMap<>(), null);
        Map<String, Permission> newPermissions = new HashMap();
        for (Iterator<Map.Entry<String, DefaultedJsonObject>> iterator = permissionMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, DefaultedJsonObject> entry = iterator.next();
            if (permissionExists(entry.getKey())) {
                continue;
            }
            Permission permission = new Permission();
            if (entry.getValue() != null) {
                if (entry.getValue().containsKey("inherits")) {
                    JsonElement inherits = entry.getValue().get("inherits");
                    List<String> inheritList = new ArrayList<>();
                    if (inherits instanceof JsonArray) {
                        ((JsonArray) inherits).forEach(it -> inheritList.add(((JsonPrimitive) it).asString()));
                    } else if (inherits instanceof JsonPrimitive) {
                        inheritList.add(((JsonPrimitive) inherits).asString());
                    }
                    permission.inheritance.addAll(inheritList);
                    for (String inherit : inheritList) {
                        if (!permissionMap.containsKey(inherit) && !existingPermissions.contains(inherit)) {
                            Thimble.LOGGER.warn(String.format("Permission \"%s\" inherits a nonexistant permission \"%s\"", entry.getKey(), inherit));
                        }
                    }
                }
            }
            newPermissions.put(entry.getKey(), permission);
        }
        permissions.putAll(newPermissions);
        defaultPermission = tree.getString("*", (String)null);
        if (defaultPermission != null && !defaultPermission.equals("null")) {
            if (!permissionMap.containsKey(defaultPermission) && !existingPermissions.contains(defaultPermission)) {
                Thimble.LOGGER.warn(String.format("Default permission is set to an undefined permission: \"%s\"", defaultPermission));
            }
        }
    }
    /*@Deprecated
    public void playersFromJson(DefaultedJsonObject map) {
        Map<String, Permission> permissionMap = mapPermissions(permissions);
        for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
            JsonElement element = entry.getValue();
            JsonArray permissions = new JsonArray();
            JsonArray removedPermissions = new JsonArray();
            if (element instanceof JsonArray) permissions = (JsonArray) element;
            else if (element instanceof JsonObject) {
                if (((JsonObject) element).containsKey("permissions")) permissions = (JsonArray) ((JsonObject) element).get("permissions");
                if (((JsonObject) element).containsKey("removed_permissions")) removedPermissions = (JsonArray) ((JsonObject) element).get("removed_permissions");
            }
            PlayerPermissionManager player = getPlayer(UUID.fromString(entry.getKey()));
            permissions.forEach(permission -> {
                if (permission instanceof JsonPrimitive) player.permission(permissionMap.get(((JsonPrimitive) permission).asString()));
            });
            removedPermissions.forEach(permission -> {
                if (permission instanceof JsonPrimitive) player.removePermission(permissionMap.get(((JsonPrimitive) permission).asString()));
            });
        }
    }*/
    public void readPlayersFromTag(CompoundTag tag) {
        List<String> permissions = getRegisteredPermissions();
        for (String key : tag.getKeys()) {
            CompoundTag entry = tag.getCompound(key);
            if (entry == null) {
                Thimble.LOGGER.warn(String.format("Permission data for player %s is equal to null. This should never happen.", key));
                continue;
            }
            PlayerPermissionManager player = getPlayer(UUID.fromString(key));
            ListTag removed = (ListTag) entry.get("removed");
            if (removed != null) {
                for (Tag removedTag: removed) {
                    if (!(removedTag instanceof StringTag)) {
                        Thimble.LOGGER.warn(String.format("A removed permission for player %s exists, but is null. This should never happen.", key));
                        continue;
                    }
                    String perm = removedTag.asString();
                    if (!permissions.contains(perm)) {
                        Thimble.LOGGER.warn(String.format("Unrecognised removed permission \"%s\" found for player %s", perm, key));
                        continue;
                    }
                    player.removePermission(perm);
                }
            }
            ListTag granted = (ListTag) entry.get("granted");
            if (granted != null) {
                for (Tag grantedTag: granted) {
                    if (!(grantedTag instanceof StringTag)) {
                        Thimble.LOGGER.warn(String.format("A permission for player %s exists, but is null. This should never happen.", key));
                        continue;
                    }
                    String perm = grantedTag.asString();
                    if (!permissions.contains(perm)) {
                        Thimble.LOGGER.warn(String.format("Unrecognised permission \"%s\" found for player %s", perm, key));
                        continue;
                    }
                    player.permission(perm);
                }
            }
        }
    }
    /*public void fromJson(DefaultedJsonObject json) {
        permissionsFromJson(DefaultedJsonObject.of((JsonObject) json.get("permissions")));
        playersFromJson(DefaultedJsonObject.of((JsonObject) json.get("players")));
    }*/
    @Override
    public String toString() {
        return permissions.keySet().toString() + "|" + permissionMap.toString();
    }
    //TODO: load the permission tree first, then deal with inheritance
    /*public void permissionsFromJson(DefaultedJsonObject json) {
        Map<String, Permission> loadedPermissionMap = new HashMap<>();
        Getter<String, Permission> permissionGetter = permission -> {
            if (loadedPermissionMap.containsKey(permission)) return loadedPermissionMap.get(permission);
            else {
                String[] mappedKeys = permission.split(".");
                Permission lastValue = null;
                for (int i = mappedKeys.length - 1; i >= 0; i--) {
                    if (loadedPermissionMap.containsKey(permission)) {
                        lastValue = loadedPermissionMap.get(permission);

                    }
                }
            }
        };
    }
    protected Map<String, Permission> findPermissionRoot(String permission, int progress, JsonObject jsonObject, Permission parent) {
        Map<String, Permission> permissionMap = new HashMap<>();
        String[] mappedKeys = permission.split(".");
        progress++;
        if (mappedKeys.length <= progress) {
            return permissionMap;
        } else if (jsonObject.containsKey(mappedKeys[progress])) {
            String string = "";
            for (int i = 0; i < progress; i++) {
                string += mappedKeys + ".";
            }
            string = string.substring(0, string.length() - 2);
            Permission perm = new Permission(mappedKeys[progress], parent);
            permissionMap.put(string, perm);
            permissionMap.putAll(findPermissionRoot(permission, progress, jsonObject.getObject(mappedKeys[progress]), perm));
        } else {
            throw new PermissionException("Permission JSON does not contain permission \"" + permission + "\".");
        }
    }
    /*protected static List<String> getInheritedPermissions(DefaultedJsonObject object) {
        List<String> perms = new ArrayList<>();
        DefaultedJsonObject children = null;
        if (object.containsKey("inherits")) {
            DefaultedJsonArray inherits = object.getArray("inherits", (DefaultedJsonArray) null);
            if (inherits != null) {
                for (int i = 0; i < inherits.size(); i++) {
                    String inherit = inherits.getString(i);
                    if (inherit != null) perms.add(inherit);
                }
            }
        } else if (object.containsKey("children")) {
            children = (DefaultedJsonObject) object.get("children", (DefaultedJsonObject) null);
        }
        if (children == null) children = object;
        for (JsonElement value : object.values()) {
            DefaultedJsonObject entry = DefaultedJsonObject.of((JsonObject) value);
            perms.addAll(getInheritedPermissions(entry));
        }
    }*/
}
