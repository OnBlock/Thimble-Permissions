package io.github.indicode.fabric.permissions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Permission {
    public PermChangeBehavior[] behaviors;
    public HashMap<String, PermissionDataManager> inheritance = new HashMap<>();

    public Permission(PermChangeBehavior... behaviors) {
        this.behaviors = behaviors;
        if (behaviors == null) this.behaviors = new PermChangeBehavior[0];
    }
}
