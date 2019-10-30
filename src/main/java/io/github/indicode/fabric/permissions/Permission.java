package io.github.indicode.fabric.permissions;

import java.util.ArrayList;
import java.util.List;

public class Permission {
    public PermChangeBehavior[] behaviors;
    public List<String> inheritance = new ArrayList<>();

    public Permission(PermChangeBehavior... behaviors) {
        this.behaviors = behaviors;
        if (behaviors == null) this.behaviors = new PermChangeBehavior[0];
    }
}
