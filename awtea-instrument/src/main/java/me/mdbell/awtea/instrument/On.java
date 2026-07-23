package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Per-method target: stacks with any detour annotation and aims that one
 * hook at the given class, overriding the class-level {@link DetourReceiver}
 * (which supplies the default target for methods without {@code @On}). A
 * detour class where <em>every</em> hook carries {@code @On} may omit
 * {@code @DetourReceiver} entirely.
 * <p>
 * This spares a whole extra detour class when a few hooks target siblings,
 * which {@link Body} makes common — a body detour binds one concrete class,
 * and a hierarchy may have several.
 * <p>
 * The hook's signature conventions follow the effective target: a leading
 * self parameter, when present, must be typed as the {@code @On} class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface On {
    /** The class this hook targets. */
    Class<?> value();
}
