package io.github.indicode.fabric.permissions;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonPrimitive;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonArray;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Indigo Amann
 */
public class PlayerPermissionManager {
    protected List<Permission> permissions = new ArrayList<>();
    protected List<Permission> removedPermissions = new ArrayList<>();
    public PermissionMap permissionMap;
    public PlayerPermissionManager(PermissionMap permissionMap) {
        this.permissionMap = permissionMap;
    }
    public boolean hasPermission(Permission permission) {
        return hasPermission(permission.getFullIdentifier());
    }
    public boolean hasPermission(String perm) {
        Permission permission = permissionMap.getPermission(perm);
        if (removedPermissions.contains(permission)) return false;
        else for (Permission removedPermission : removedPermissions) {
            if (permission.isDescendantOf(removedPermission)) return false;
            else if (permissions.contains(permission)) return true;
            else for (Permission here : permissions) {
                    if (permission.isDescendantOf(here)) return true;
                }
        }
        return false;
    }
    public PlayerPermissionManager removePermission(Permission permission) {
        if (!removedPermissions.contains(permission)) {
            permissions.remove(permission);
            removedPermissions.add(permission);
        }
        return this;
    }
    public PlayerPermissionManager permission(Permission permission) {
        if (permission == null) return this;
        if (permissions.contains(permission)) return this;
        for (Iterator<Permission> iterator = permissions.iterator(); iterator.hasNext(); ) {
            Permission here = iterator.next();
            if (permission.isDescendantOf(here)) return this;
            if (here.isDescendantOf(permission)) iterator.remove();
        }
        this.permissions.add(permission);
        this.permissionMap.addGroup(permission);
        return this;
    }
    public JsonElement toJson() {
        if (removedPermissions.isEmpty()) {
            DefaultedJsonArray permArray = new DefaultedJsonArray();
            permissions.forEach(permission -> permArray.add(new JsonPrimitive(permission.getFullIdentifier())));
            return permArray;
        } else {
            DefaultedJsonObject jsonObject = new DefaultedJsonObject();
            DefaultedJsonArray rpermArray = new DefaultedJsonArray();
            removedPermissions.forEach(perm -> rpermArray.add(new JsonPrimitive(perm)));
            jsonObject.set("removed_permissions", rpermArray);
            if (!permissions.isEmpty()) {
                DefaultedJsonArray groupArray = new DefaultedJsonArray();
                permissions.forEach(permission -> groupArray.add(new JsonPrimitive(permission.getFullIdentifier())));
                jsonObject.set("permissions", groupArray);
            }
            return jsonObject;
        }
    }
    @Override
    public String toString() {
        return permissions.toString();
    }
}
