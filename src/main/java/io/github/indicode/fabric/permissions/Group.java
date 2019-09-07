package io.github.indicode.fabric.permissions;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Indigo Amann
 */
public class Group {
    public Identifier identifier;
    public List<String> permissions = new ArrayList<>();
    public List<Group> inheritance = new ArrayList<>();
    public Group(Identifier identifier) {
        this.identifier = identifier;
    }
    public Group permission(String string) {
        this.permissions.add(string);
        return this;
    }
    public Group inherit(Group group) {
        this.inheritance.add(group);
        return this;
    }
    public boolean hasPermission(String permission) {
        if (permissions.contains(permission)) return true;
        else for (Group group : inheritance) {
            if (group.hasPermission(permission)) return true;
        }
        return false;
    }
}
