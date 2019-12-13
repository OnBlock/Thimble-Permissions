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
    protected List<String> revokedPermissions = new ArrayList<>();
    public PermissionMap permissionMap;
    public PlayerPermissionManager(PermissionMap permissionMap) {
        this.permissionMap = permissionMap;
    }
    public boolean hasPermission(String permission) {
        if (revokedPermissions.contains(permission)) return false;
        if (permissions.contains("*")) return true;
        if (permissionMap.defaultPermissionMatches(permission)) return true;
        if (permissionMap.isInherited(permissionMap.defaultPermission, permission)) return true;
        if (permissions.contains(permission)) return true;
        for (String here : permissions) {
            if (permissionMap.isInherited(here, permission)) return true;
        }
        return false;
    }
    public boolean hasExactPermission(String permission) {
        if (revokedPermissions.contains(permission)) return false;
        return permissions.contains(permission);
    }
    public PlayerPermissionManager revokePermission(String permission) {
        permissions.remove(permission);
        if (!revokedPermissions.contains(permission)) {
            revokedPermissions.add(permission);
        }
        return this;
    }
    public PlayerPermissionManager resetPermission(String permission) {
        permissions.remove(permission);
        revokedPermissions.remove(permission);
        return this;
    }
    public PlayerPermissionManager grantPermission(String permission) {
        if (permission == null) return this;
        revokedPermissions.remove(permission);
        if (permissions.contains(permission)) return this;
        if (!permission.equals("*")) {
            for (Iterator<String> iterator = permissions.iterator(); iterator.hasNext(); ) {
                String here = iterator.next();
                if (permissionMap.isInherited(here, permission)) return this;
                if (permissionMap.isInherited(permission, here)) iterator.remove();
            }
        }
        this.permissions.add(permission);
        return this;
    }
    @Deprecated
    public JsonElement toJson() {
        if (revokedPermissions.isEmpty()) {
            DefaultedJsonArray permArray = new DefaultedJsonArray();
            permissions.forEach(permission -> permArray.add(new JsonPrimitive(permission)));
            return permArray;
        } else {
            DefaultedJsonObject jsonObject = new DefaultedJsonObject();
            DefaultedJsonArray rpermArray = new DefaultedJsonArray();
            revokedPermissions.forEach(perm -> {
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
        if (!revokedPermissions.isEmpty()) {
            ListTag removed = new ListTag();
            revokedPermissions.forEach(permission -> removed.add(StringTag.of(permission)));
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
