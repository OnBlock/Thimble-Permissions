package io.github.indicode.fabric.permissions.command;

import io.github.indicode.fabric.permissions.Permission;

/**
 * @author Indigo Amann
 */
public class NoSavePermission extends Permission {
    public NoSavePermission(String identifier) {
        super(identifier);
    }

    public NoSavePermission(String identifier, Permission permission) {
        super(identifier, permission);
    }
    @Override
    public boolean shouldSave() {
        return false;
    }
}
