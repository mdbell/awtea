package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts a detour to sites inside the given classes. Stacks with any
 * detour annotation on the same method — {@link DetourMethod},
 * {@link Before}/{@link After}, {@link Guard}, {@link Filter}, and the
 * field/element hooks — turning "this callee has several call sites but I
 * mean the one in X" from a runtime conditional into configuration.
 * <p>
 * Classes are referenced by literal, so renames refactor with the code; and
 * a filtered detour still participates in zero-match verification, so a call
 * site that migrates out of the allowed set fails the build instead of
 * silently unbinding.
 * <p>
 * Granularity is the enclosing class, not the enclosing method — method
 * names in this ecosystem are rename-churned strings, and class-level covers
 * the redirect-style cases without adding another string binding.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Callers {
    /** Classes whose sites this detour may bind. Must be non-empty. */
    Class<?>[] value();
}
