package io.github.indicode.fabric.permissions;

/**
 * @author Indigo Amann
 */
public class PermissionException extends RuntimeException {
    public PermissionException(String details) {
        super(details);
    }
}
