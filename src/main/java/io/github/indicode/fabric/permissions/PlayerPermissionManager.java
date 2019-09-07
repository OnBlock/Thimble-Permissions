package io.github.indicode.fabric.permissions;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Indigo Amann
 */
public class PlayerPermissionManager {
    protected List<String> addedPermissions = new ArrayList<>();
    protected List<String> removedPermissions = new ArrayList<>();
    protected List<Group> groups = new ArrayList<>();
    public boolean hasPermission(String permission) {
        if (removedPermissions.contains(permission)) return false;
        else if (addedPermissions.contains(permission)) return true;
        else return groupHasPermission(permission) != null;
    }
    public Group groupHasPermission(String permission) {
        for (Group group : groups) {
            if (group.hasPermission(permission)) return group;
        }
        return null;
    }
    public void permission(String permission) {
        if (removedPermissions.contains(permission)) removedPermissions.remove(permission);
        if (!hasPermission(permission)) addedPermissions.add(permission);
    }
    public void removePermission(String permission) {
        if (!removedPermissions.contains(permission)) {
            if (addedPermissions.contains(permission)) addedPermissions.remove(permission);
            else removedPermissions.add(permission);
        }
    }
    public void group(Group group) {
        if (!groups.contains(group)) groups.add(group);
    }
}
