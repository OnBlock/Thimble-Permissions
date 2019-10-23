package io.github.indicode.fabric.permissions;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonPrimitive;
import io.github.indicode.fabric.permissions.command.CommandPermission;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonArray;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

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
        }
        if (permissions.contains(permission)) return true;
        else for (Permission here : permissions) {
            if (here.hasPermission(permission)) return true;
        }
        return false;
    }
    public boolean hasPermissionOrChild(String perm) {
        Permission permission = permissionMap.getPermission(perm);
        if (removedPermissions.contains(permission)) return false;
        else for (Permission removedPermission : removedPermissions) {
            if (permission.isDescendantOf(removedPermission)) return false;
        }
        if (permissions.contains(permission)) return  true;
        for (Permission permission1: permissions) {
            if (permission1.hasPermission(permission))
            if (permission.hasPermission(permission1)) return true;
        }
        return false;
    }
    public PlayerPermissionManager removePermission(Permission permission) {
        if (permissions.contains(permission)) permissions.remove(permission);
        else if (!removedPermissions.contains(permission)) {
            removedPermissions.add(permission);
        }
        return this;
    }
    public PlayerPermissionManager permission(Permission permission) {
        if (permission == null) return this;
        if (removedPermissions.contains(permission)) removedPermissions.remove(permission);
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
    @Deprecated
    public JsonElement toJson() {
        if (removedPermissions.isEmpty()) {
            DefaultedJsonArray permArray = new DefaultedJsonArray();
            permissions.forEach(permission -> permArray.add(new JsonPrimitive(permission.getFullIdentifier())));
            return permArray;
        } else {
            DefaultedJsonObject jsonObject = new DefaultedJsonObject();
            DefaultedJsonArray rpermArray = new DefaultedJsonArray();
            removedPermissions.forEach(perm -> {
                if (perm != null) rpermArray.add(new JsonPrimitive(perm.getFullIdentifier()));
            });
            jsonObject.set("removed_permissions", rpermArray);
            if (!permissions.isEmpty()) {
                DefaultedJsonArray groupArray = new DefaultedJsonArray();
                permissions.forEach(permission -> groupArray.add(new JsonPrimitive(permission.getFullIdentifier())));
                jsonObject.set("permissions", groupArray);
            }
            return jsonObject;
        }
    }
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        if (!removedPermissions.isEmpty()) {
            ListTag removed = new ListTag();
            removedPermissions.forEach(permission -> removed.add(StringTag.of(permission.identifier)));
            tag.put("removed", removed);
        }
        if (!permissions.isEmpty()) {
            ListTag granted = new ListTag();
            permissions.forEach(permission -> granted.add(StringTag.of(permission.identifier)));
            tag.put("granted", granted);
        }
        return tag;
    }
    @Override
    public String toString() {
        return permissions.toString();
    }

}
