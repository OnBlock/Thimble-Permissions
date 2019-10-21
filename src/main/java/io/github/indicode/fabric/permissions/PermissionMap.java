package io.github.indicode.fabric.permissions;

import blue.endless.jankson.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonObject;
import net.minecraft.util.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * @author Indigo Amann
 */
public class PermissionMap {
    protected Map<UUID, PlayerPermissionManager> permissionMap = new HashMap<>();
    protected List<Permission> permissions = new ArrayList<>();
    public ImmutableList<Permission> getRegisteredPermissions() {
        return ImmutableList.copyOf(permissions);
    }
    public PlayerPermissionManager getPlayer(UUID uuid) {
        if (permissionMap.containsKey(uuid)) return permissionMap.get(uuid);
        PlayerPermissionManager manager = new PlayerPermissionManager(this);
        permissionMap.put(uuid, manager);
        return manager;
    }
    public boolean addGroup(Permission permission) {
        if (permission == null) return  false;
        while (permission.parent != null) {
            permission = permission.parent;
        }
        if (!permissions.contains(permission)) {
            for (Iterator<Permission> iterator = permissions.iterator(); iterator.hasNext(); ) {
                Permission value = iterator.next();
                if (permission.isDescendantOf(value)) return false;
                else if (value.isDescendantOf(permission)) iterator.remove();
            }
            permissions.add(permission);
            permission.getInheritance().forEach(this::addGroup);
            return true;
        }
        return false;
    }
    public Permission getPermission(Permission permission) {
        if (addGroup(permission)) {
            return permission;
        } else {
            return getPermission(permission.getFullIdentifier());
        }
    }
    public <T extends Permission> T getPermission(String permission, Class<? extends T> def) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Map<String, Permission> map =  mapPermissions(getRegisteredPermissions());
        if (map.containsKey(permission) && map.get(permission).getClass().isAssignableFrom(def)) {
            return (T) map.get(permission);
        } else {
            String[] split = permission.split("[.]");
            String t = split[split.length - 1];
            T instance = split.length > 1 ?
                    def.getConstructor(String.class, Permission.class).newInstance(t, getPermission(permission.substring(0, permission.length() - t.length() - 1))) :
                    def.getConstructor(String.class, Permission.class).newInstance(permission, null);
            map.put(permission, instance);
            return instance;
        }
    }
    public Permission getPermission(String name) {
        Map<String, Permission> map = mapPermissions(permissions);
        if (map.containsKey(name)) return map.get(name);
        else {
            String[] nameSplit = name.split("[.]");
            String lastPerm = null;
            int i;
            for (i = nameSplit.length - 1; i >= 0; i--) {
                String nameSlice = "";
                for (int j = 0; j < i; j++) {
                    nameSlice += (j == 0 ? "" : ".") + nameSplit[j];
                }
                if (map.containsKey(nameSlice)) {
                    lastPerm = nameSlice;
                    break;
                }
            }
            Permission current;
            if (lastPerm == null) {
                current = new Permission(nameSplit[0]);
                addGroup(current);
                i = 1;
            } else current = map.get(lastPerm);
            while(i < nameSplit.length) {
                current = new Permission(nameSplit[i], current);
                i++;
            }
            return current;
        }
    }
    public boolean hasPermission(Permission permission, UUID player) {
        return getPlayer(player).hasPermission(permission);
    }
    public boolean hasPermission(String permission, UUID player) {
        return getPlayer(player).hasPermission(permission);
    }
    public boolean hasPermissionOrChild(String permission, UUID player) {
        return getPlayer(player).hasPermissionOrChild(permission);
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
            if (permission.shouldSave()) jsonObject.set(permission.identifier, permission.toJson());
        }
        return jsonObject;
    }
    public DefaultedJsonObject playersToJson() {
        DefaultedJsonObject jsonObject = new DefaultedJsonObject();
        permissionMap.forEach((uuid, manager) -> {
            jsonObject.set(uuid.toString(), manager.toJson());
        });
        return jsonObject;
    }
    public Map<String, Permission> mapPermissions(List<Permission> permissions) {
        Map<String, Permission> map = new HashMap<>();
        for (Permission permission : permissions) {
            map.put(permission.getFullIdentifier(), permission);
            map.putAll(mapPermissions(permission.getChildren()));
        }
        return map;
    }
    protected Map<String, Pair<Permission, DefaultedJsonObject>> loadBlankPermissionTree(DefaultedJsonObject tree, Map<String, Pair<Permission, DefaultedJsonObject>> existingPermissions, String nestLevel, Permission parent) {
        Map<String, Pair<Permission, DefaultedJsonObject>> keyMap = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : tree.entrySet()) {
            String id = nestLevel == null ? entry.getKey() : nestLevel + "." + entry.getKey();
            if (entry.getValue() instanceof JsonObject) {
                Permission permission;
                if (!existingPermissions.containsKey(id)) {
                    permission = new Permission(entry.getKey(), parent);
                } else {
                    permission = existingPermissions.get(id).getLeft();
                }
                keyMap.put(id, new Pair<>(permission, DefaultedJsonObject.of((JsonObject) entry.getValue())));
                keyMap.putAll(loadBlankPermissionTree(DefaultedJsonObject.of((JsonObject) entry.getValue()), keyMap, id, permission));
            } else if (entry.getValue() == null || entry.getValue().equals(JsonNull.INSTANCE)) {
                Permission permission;
                if (!existingPermissions.containsKey(id)) {
                    permission = new Permission(entry.getKey(), parent);
                } else {
                    permission = existingPermissions.get(id).getLeft();
                }
                keyMap.put(id, new Pair<>(permission, null));
            }
        }
        return keyMap;
    }
    public void permissionsFromJson(DefaultedJsonObject tree) {
        Map<String, Permission> existingPermissionMap = mapPermissions(permissions);
        Map<String, Pair<Permission, DefaultedJsonObject>> permissionMap = loadBlankPermissionTree(tree, new HashMap<>(), null, null);
        for (Iterator<Map.Entry<String, Pair<Permission, DefaultedJsonObject>>> iterator = permissionMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, Pair<Permission, DefaultedJsonObject>> entry = iterator.next();
            if (existingPermissionMap.containsKey(entry.getKey())) {
                iterator.remove();
                continue;
            }
            if (entry.getValue().getRight() == null) {
                continue;
            }
            if (entry.getValue().getRight().containsKey("inherits")) {
                JsonElement inherits = entry.getValue().getRight().get("inherits");
                List<String> inheritList = new ArrayList<>();
                if (inherits instanceof JsonArray) {
                    ((JsonArray) inherits).forEach(it -> inheritList.add(((JsonPrimitive) it).asString()));
                } else if (inherits instanceof JsonPrimitive) {
                    inheritList.add(((JsonPrimitive) inherits).asString());
                }
                for (String inherit : inheritList) {
                    if (permissionMap.containsKey(inherit)) {
                        entry.getValue().getLeft().inherit(permissionMap.get(inherit).getLeft());
                    } else {
                        Thimble.LOGGER.warn(String.format("Permission \"%s\" inherits a nonexistant permission \"%s\"", entry.getValue().getLeft().getFullIdentifier(), inherit));
                    }
                }
            }
        }
        permissionMap.values().forEach(pair -> addGroup(pair.getLeft()));
    }
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
    }
    public void fromJson(DefaultedJsonObject json) {
        permissionsFromJson(DefaultedJsonObject.of((JsonObject) json.get("permissions")));
        playersFromJson(DefaultedJsonObject.of((JsonObject) json.get("players")));
    }
    @Override
    public String toString() {
        return permissions.toString() + "|" + permissionMap.toString();
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
