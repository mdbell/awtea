package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Result-mutating detour: {@link DetourHacks} inserts a call to this method
 * immediately after every matched call site, feeding it the original's return
 * value as the trailing parameter; whatever the filter returns becomes the
 * call's result. Return the parameter unchanged to observe without mutating.
 * <p>
 * Conventions:
 * <ul>
 * <li>The filter must be {@code public static}, returning the original's
 * return type, with that same type as its trailing parameter.</li>
 * <li>Instance original {@code target.foo(a, b) : R}: filter signature is
 * {@code static R filter(Target self, A a, B b, R result)}.</li>
 * <li>Static original {@code Target.foo(a, b) : R}: filter signature is
 * {@code static R filter(A a, B b, R result)}.</li>
 * <li>Void originals cannot be filtered — use {@link After}.</li>
 * <li>Runs only on normal completion; an exception in the original skips it.
 * Filters compose with replacements ({@link DetourMethod}) and with each
 * other, but not with a {@link Guard} on the same call site.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Filter {
    /**
     * Original method name. If empty, the original name is assumed to be the
     * same as the filter method name.
     */
    String value() default "";
}
