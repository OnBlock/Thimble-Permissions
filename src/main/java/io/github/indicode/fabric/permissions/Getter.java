package io.github.indicode.fabric.permissions;

/**
 * @author Indigo Amann
 */
public interface Getter<A, B> {
    B get(A a);
}
