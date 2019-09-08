package io.github.indicode.fabric.permissions;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonNull;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonArray;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonObject;

import java.util.*;

/**
 * @author Indigo Amann
 */
public class Permission {
    public final String identifier;
    public Permission parent;
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
    public JsonElement toJson() {
        if (inheritance.isEmpty() && children.isEmpty()) return JsonNull.INSTANCE;
        DefaultedJsonObject jsonObject = new DefaultedJsonObject();
        if (!inheritance.isEmpty()) {
            if (inheritance.size() == 1) jsonObject.set("inherits", new JsonPrimitive(inheritance.get(0).getFullIdentifier()));
            else {
                DefaultedJsonArray inheritsArray = new DefaultedJsonArray();
                for (Permission permission : inheritance) {
                    inheritsArray.add(new JsonPrimitive(permission.getFullIdentifier()));
                }
                jsonObject.set("inherits", inheritsArray);
            }
        }
        if (!children.isEmpty()) {
            for (Permission permission : children) {
                jsonObject.set(permission.identifier, permission.toJson());
            }
        }
        return jsonObject;
    }
    public void fromJson(JsonElement json, Getter<String, Permission> permissionGetter) {
        if (json != null && !json.equals(JsonNull.INSTANCE) && json instanceof DefaultedJsonObject) {
            DefaultedJsonObject jsonObject = (DefaultedJsonObject) json;
            DefaultedJsonArray inheritanceArray = jsonObject.getArray("inherits", (DefaultedJsonArray) null);
            DefaultedJsonObject childMap = inheritanceArray == null ? jsonObject.containsKey("children") ? DefaultedJsonObject.of((JsonObject) jsonObject.get("children")) : jsonObject : jsonObject;
            if (inheritanceArray != null) {
                for (int i = 0; i < inheritanceArray.size(); i++) {
                    String name = inheritanceArray.getString(i);
                    if (name != null) inheritance.add(permissionGetter.get(name));
                }
            }
            for (Map.Entry<String, JsonElement> entry : childMap.entrySet()) {

            }
        }
    }
    public String getFullIdentifier() {
        String name = identifier;
        Permission permission = this;
        while (permission.parent != null) {
            permission = permission.parent;
            name = permission.identifier + "." + name;
        }
        return name;
    }
    @Override
    public String toString() {
        return "Permission[id=" + getFullIdentifier() + ", inherits=" + inheritance.toString() + ", children=" + children.toString() + "]";
    }
}
