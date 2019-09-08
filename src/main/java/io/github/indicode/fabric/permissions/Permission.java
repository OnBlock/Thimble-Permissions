package io.github.indicode.fabric.permissions;

import blue.endless.jankson.JsonPrimitive;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonArray;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonObject;

import java.util.*;

/**
 * @author Indigo Amann
 */
public class Permission {
    public final String identifier;
    public final Permission parent;
    protected ArrayList<Permission> children = new ArrayList<>();
    protected ArrayList<Permission> inheritance = new ArrayList<>();
    public Permission(String identifier) {
        this(identifier, null);
    }
    public Permission(String identifier, Permission permission) {
        this.identifier = identifier;
        this.parent = permission;
        if (parent != null) parent.child(this);
    }
    public Permission child(Permission permission) {
        if (children.contains(permission)) return this;
        for (Permission here : children) {
            if (permission.isDescendantOf(here)) return this;
            if (here.isDescendantOf(permission)) children.remove(here);
        }
        this.children.add(permission);
        return this;
    }
    public Permission inherit(Permission permission) {
        if (hasPermission(permission)) return this;
        if (inheritance.contains(permission)) return this;
        for (Permission here : inheritance) {
            if (permission.isDescendantOf(here)) return this;
            if (here.isDescendantOf(permission)) inheritance.remove(here);
        }
        this.inheritance.add(permission);
        return this;
    }
    public List<Permission> getChildren() {
        return children;
    }
    public List<Permission> getInheritance() {
        return inheritance;
    }
    public boolean isDescendantOf(Permission permission) {
        if (parent == null) return false;
        else if (this.parent.equals(permission)) return true;
        else return this.parent.isDescendantOf(permission);
    }
    public boolean hasPermission(Permission permission) {
        if (children.contains(permission)) return true;
        else if (inheritance.contains(permission)) return true;
        for (Permission here : children) {
            if (permission.isDescendantOf(here)) return true;
        }
        for (Permission here : inheritance) {
            if (permission.isDescendantOf(here)) return true;
        }
        return false;
    }
    public DefaultedJsonObject toJson() {
        DefaultedJsonObject jsonObject = new DefaultedJsonObject();
        if (!inheritance.isEmpty()) {
            DefaultedJsonArray inheritsArray = new DefaultedJsonArray();
            for (Permission permission : inheritance) {
                inheritsArray.add(new JsonPrimitive(permission.toString()));
            }
            jsonObject.set("inherits", inheritsArray);
        }
        if (!children.isEmpty()) {
            if (inheritance.isEmpty()) {
                for (Permission permission : children) {
                    jsonObject.set(permission.identifier, permission.toJson());
                }
            } else {
                DefaultedJsonObject childArray = new DefaultedJsonObject();
                for (Permission permission : children) {
                    childArray.set(permission.identifier, permission.toJson());
                }
                jsonObject.set("children", childArray);
            }
        }
        return jsonObject;
    }
    public String toString() {
        String name = identifier;
        Permission permission = this;
        while (permission.parent != null) {
            permission = permission.parent;
            name = permission.identifier + "." + name;
        }
        return name;
    }
}
