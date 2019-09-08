package io.github.indicode.fabric.permissions;

import io.github.indicode.fabric.tinyconfig.DefaultedJsonObject;

import java.util.*;

/**
 * @author Indigo Amann
 */
public class PermissionMap {
    public static final PermissionMap INSTANCE = new PermissionMap();
    public Map<UUID, PlayerPermissionManager> permissionMap = new HashMap<>();
    protected List<Permission> groups = new ArrayList<>();
    public PlayerPermissionManager getPlayer(UUID uuid) {
        if (permissionMap.containsKey(uuid)) return permissionMap.get(uuid);
        PlayerPermissionManager manager = new PlayerPermissionManager(this);
        permissionMap.put(uuid, manager);
        return manager;
    }
    public void addGroup(Permission permission) {
        if (!groups.contains(permission)) {
            for (Iterator<Permission> iterator = groups.iterator(); iterator.hasNext(); ) {
                Permission value = iterator.next();
                if (permission.isDescendantOf(value)) return;
                else if (value.isDescendantOf(permission)) iterator.remove();
            }
            groups.add(permission);
            permission.getInheritance().forEach(this::addGroup);
        }
    }
    public boolean hasPermission(Permission permission, UUID player) {
        return getPlayer(player).hasPermission(permission);
    }
    public DefaultedJsonObject toJson() {
        DefaultedJsonObject jsonObject = new DefaultedJsonObject();
        DefaultedJsonObject playerMap = new DefaultedJsonObject();
        permissionMap.forEach((uuid, manager) -> playerMap.set(uuid.toString(), manager.toJson()));
        jsonObject.set("players", playerMap);
        DefaultedJsonObject groupMap = new DefaultedJsonObject();
        for (Permission permission : groups) {
            groupMap.set(permission.identifier, permission.toJson());
        }
        jsonObject.set("permissions", groupMap);
        return jsonObject;
    }
}
