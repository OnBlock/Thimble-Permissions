package io.github.indicode.fabric.permissions;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonPrimitive;
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
    protected List<String> permissions = new ArrayList<>();
    protected List<String> removedPermissions = new ArrayList<>();
    public PermissionMap permissionMap;
    public PlayerPermissionManager(PermissionMap permissionMap) {
        this.permissionMap = permissionMap;
    }
    public boolean hasPermission(Permission permission) {
        return hasPermission(permission.getFullIdentifier());
    }
    public boolean hasPermission(String permission) {
        if (removedPermissions.contains(permission)) return false;
        if (permissionMap.defaultPermission.equals(permission)) return true;
        if (permissionMap.isInherited(permissionMap.defaultPermission, permission)) return true;
        if (permissions.contains(permission)) return true;
        for (String here : permissions) {
            if (permissionMap.isInherited(here, permission)) return true;
        }
        for (String removedPermission : removedPermissions) {
            if (permissionMap.isInherited(removedPermission, permission)) return false;
        }
        return false;
    }
    public PlayerPermissionManager removePermission(String permission) {
        if (permissions.contains(permission)) permissions.remove(permission);
        else if (!removedPermissions.contains(permission)) {
            removedPermissions.add(permission);
        }
        return this;
    }
    public PlayerPermissionManager permission(String permission) {
        if (permission == null) return this;
        if (removedPermissions.contains(permission)) removedPermissions.remove(permission);
        if (permissions.contains(permission)) return this;
        for (Iterator<String> iterator = permissions.iterator(); iterator.hasNext(); ) {
            String here = iterator.next();
            if (permissionMap.isInherited(here, permission)) return this;
            if (permissionMap.isInherited(permission, here)) iterator.remove();
        }
        this.permissions.add(permission);
        this.permissionMap.getPermission(permission);
        return this;
    }
    @Deprecated
    public JsonElement toJson() {
        if (removedPermissions.isEmpty()) {
            DefaultedJsonArray permArray = new DefaultedJsonArray();
            permissions.forEach(permission -> permArray.add(new JsonPrimitive(permission)));
            return permArray;
        } else {
            DefaultedJsonObject jsonObject = new DefaultedJsonObject();
            DefaultedJsonArray rpermArray = new DefaultedJsonArray();
            removedPermissions.forEach(perm -> {
                if (perm != null) rpermArray.add(new JsonPrimitive(perm));
            });
            jsonObject.set("removed_permissions", rpermArray);
            if (!permissions.isEmpty()) {
                DefaultedJsonArray groupArray = new DefaultedJsonArray();
                permissions.forEach(permission -> groupArray.add(new JsonPrimitive(permission)));
                jsonObject.set("permissions", groupArray);
            }
            return jsonObject;
        }
    }
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        if (!removedPermissions.isEmpty()) {
            ListTag removed = new ListTag();
            removedPermissions.forEach(permission -> removed.add(StringTag.of(permission)));
            tag.put("removed", removed);
        }
        if (!permissions.isEmpty()) {
            ListTag granted = new ListTag();
            permissions.forEach(permission -> granted.add(StringTag.of(permission)));
            tag.put("granted", granted);
        }
        return tag;
    }
    @Override
    public String toString() {
        return permissions.toString();
    }

}
