package io.github.indicode.fabric.permissions;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonPrimitive;
import com.google.common.collect.ImmutableList;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonArray;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @author Indigo Amann
 */
public class PlayerPermissionManager {
    protected HashMap<String, PermissionDataManager> specialData = new HashMap<>();
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
    public ImmutableList<String> getPermissions() {
        return ImmutableList.copyOf(permissions);
    }
    public ImmutableList<String> getRevokedPermissions() {
        return ImmutableList.copyOf(revokedPermissions);
    }
    public boolean hasExactPermission(String permission) {
        if (revokedPermissions.contains(permission)) return false;
        return permissions.contains(permission);
    }
    public boolean isDeniedPermission(String permission) {
        return revokedPermissions.contains(permission);
    }
    public PlayerPermissionManager revokePermission(String permission) {
        permissions.remove(permission);
        specialData.remove(permission);
        if (!revokedPermissions.contains(permission)) {
            revokedPermissions.add(permission);
        }
        return this;
    }
    public PlayerPermissionManager resetPermission(String permission) {
        permissions.remove(permission);
        revokedPermissions.remove(permission);
        specialData.remove(permission);
        return this;
    }
    public PlayerPermissionManager grantPermission(String permission) {
        if (permission == null) return this;
        revokedPermissions.remove(permission);
        specialData.remove(permission);
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
    public PermissionDataManager getDataManager(String permission) {
        return specialData.get(permission);
    }
    public void setDataManager(String permission, PermissionDataManager manager) {
        specialData.put(permission, manager);
    }
    public PermissionDataManager getOrCreateDataManager(String permission) {
        PermissionDataManager dm = getDataManager(permission);
        if (dm == null) {
            dm = new PermissionDataManager();
            setDataManager(permission, dm);
        }
        return dm;
    }
    public boolean hasDataManager(String permision) {
        return getDataManager(permision) != null;
    }
    public void setSpecialData(String permission, String key, String value) {
        getOrCreateDataManager(permission).setData(key, value);
    }
    public String getSpecialData(String permission, String key) {
        PermissionDataManager dm = getDataManager(permission);
        return dm == null ? null : dm.getData(key);
    }
    public boolean hasSpecialData(String permission, String key) {
        return getSpecialData(permission, key) != null;
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
