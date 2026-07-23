package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Conditional-bypass detour: {@link DetourHacks} calls this method before
 * every matched call site and lets it decide whether the original runs. The
 * guard receives an {@link Interception} as its trailing parameter — leave it
 * untouched and the original call proceeds as normal; {@code cancel(...)} it
 * and the original is skipped, with the cancel value substituted as the
 * call's result.
 * <p>
 * Conventions (as {@link Before}, plus the trailing carrier):
 * <ul>
 * <li>The guard method must be {@code public static void}, with a trailing
 * {@link Interception} parameter.</li>
 * <li>Instance original {@code target.foo(a, b)}: guard signature is
 * {@code (Target self, A a, B b, Interception ci)}.</li>
 * <li>Static original {@code Target.foo(a, b)}: guard signature is
 * {@code (A a, B b, Interception ci)}.</li>
 * <li>Call sites are matched by method name and parameter types; the
 * original's return type does not participate — its concrete type is
 * recovered per call site when the substituted value is unboxed/cast.</li>
 * <li>Constructors are not supported. A call site may have at most one
 * guard, and a guard cannot be combined with a replacement
 * ({@link DetourMethod}) or a {@link Filter} on the same call site;
 * {@link Before}/{@link After} advice compose fine and run only when the
 * original actually runs.</li>
 * </ul>
 * When you need try/finally semantics around the original (e.g. restoring
 * state even when it throws), a replacement {@link DetourMethod} that calls
 * through remains the right tool — a guard cannot express that.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Guard {
    /**
     * Original method name. If empty, the original name is assumed to be the
     * same as the guard method name.
     */
    String value() default "";
}
