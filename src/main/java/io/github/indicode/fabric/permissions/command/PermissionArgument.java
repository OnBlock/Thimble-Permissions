package io.github.indicode.fabric.permissions.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.indicode.fabric.permissions.Permission;
import io.github.indicode.fabric.permissions.PermissionMap;
import io.github.indicode.fabric.tinyconfig.Getter;
import net.minecraft.text.LiteralText;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * @author Indigo Amann
 */
public class PermissionArgument implements ArgumentType<Permission> {
    public static DynamicCommandExceptionType INVALID_PERMISSION_EXCEPTION = new DynamicCommandExceptionType(object_1 -> new LiteralText("Permission \"" + object_1 + "\" does not exist."));
    public PermissionArgument(Getter<PermissionMap> permissionMap) {
        this.permissions = permissionMap;
    }
    protected Getter<PermissionMap> permissions;
    @Override
    public Permission parse(StringReader reader) throws CommandSyntaxException {
        String permid = reader.readString();
        Permission permission = permissions.get().mapPermissions(permissions.get().getRegisteredPermissions()).get(permid);
        if (permission == null) throw INVALID_PERMISSION_EXCEPTION.create(permid);
        return permission;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        permissions.get().mapPermissions(permissions.get().getRegisteredPermissions()).keySet().forEach(it -> {
            System.out.println(builder.getRemaining());
            if (it.startsWith(builder.getRemaining())) builder.suggest(it);
        });
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return new ArrayList<>();
    }
}
