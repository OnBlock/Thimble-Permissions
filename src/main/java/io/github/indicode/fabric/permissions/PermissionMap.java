package io.github.indicode.fabric.permissions;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import com.google.common.collect.ImmutableMap;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonObject;

import java.util.*;

/**
 * @author Indigo Amann
 */
public class PermissionMap {
    public Map<UUID, PlayerPermissionManager> permissionMap = new HashMap<>();
    protected List<Permission> permissions = new ArrayList<>();
    protected ImmutableMap<String, Permission> cachedPermissionTree = null;
    protected boolean cacheDirty = true;
    public PlayerPermissionManager getPlayer(UUID uuid) {
        if (permissionMap.containsKey(uuid)) return permissionMap.get(uuid);
        PlayerPermissionManager manager = new PlayerPermissionManager(this);
        permissionMap.put(uuid, manager);
        return manager;
    }
    public void addGroup(Permission permission) {
        while (permission.parent != null) {
            permission = permission.parent;
        }
        if (!permissions.contains(permission)) {
            for (Iterator<Permission> iterator = permissions.iterator(); iterator.hasNext(); ) {
                Permission value = iterator.next();
                if (permission.isDescendantOf(value)) return;
                else if (value.isDescendantOf(permission)) iterator.remove();
            }
            permissions.add(permission);
            permission.getInheritance().forEach(this::addGroup);
            cacheDirty = true;
        }
    }
    public boolean hasPermission(Permission permission, UUID player) {
        return getPlayer(player).hasPermission(permission);
    }
    public DefaultedJsonObject toJson() {
        DefaultedJsonObject jsonObject = new DefaultedJsonObject();
        jsonObject.set("permissions", permissionsToJson());
        jsonObject.set("players", playersToJson());
        return jsonObject;
    }
    public DefaultedJsonObject permissionsToJson() {
        DefaultedJsonObject jsonObject = new DefaultedJsonObject();
        for (Permission permission : permissions) {
            jsonObject.set(permission.identifier, permission.toJson());
        }
        return jsonObject;
    }
    public DefaultedJsonObject playersToJson() {
        DefaultedJsonObject jsonObject = new DefaultedJsonObject();
        permissionMap.forEach((uuid, manager) -> jsonObject.set(uuid.toString(), manager.toJson()));
        return jsonObject;
    }
    public ImmutableMap<String, Permission> treeExistingPermissions(List<Permission> permissions) {
        if (!cacheDirty && cachedPermissionTree != null) return cachedPermissionTree;
        Map<String, Permission> map = new HashMap<>();
        for (Permission permission : permissions) {
            map.put(permission.toString(), permission);
            map.putAll(treeExistingPermissions(permission.getChildren()));
        }
        cachedPermissionTree = ImmutableMap.copyOf(map);
        return cachedPermissionTree;
    }
    protected Map<String, Permission> loadBlankPermissionTree(DefaultedJsonObject tree, Map<String, Permission> existingPermissions, String nestLevel, Permission parent) {
        Map<String, Permission> keyMap = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : tree.entrySet()) {
            if (!(entry.getValue() instanceof JsonObject)) continue;
            String id = nestLevel == null ? entry.getKey() : nestLevel + "." + entry.getKey();
            Permission permission;
            if (!existingPermissions.containsKey(id)) {
                permission = new Permission(entry.getKey(), parent);
            } else {
                permission = existingPermissions.get(id);
            }
            keyMap.put(id, permission);
            keyMap.putAll(loadBlankPermissionTree(DefaultedJsonObject.of((JsonObject) entry.getValue()), keyMap, id, permission));
        }
        return keyMap;
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
