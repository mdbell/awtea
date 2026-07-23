package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Advice variant of {@link DetourMethod}: instead of replacing calls to the
 * original method, {@link DetourHacks} inserts a call to this method
 * immediately before every matched call site, leaving the original call in
 * place. Use this when a detour only needs to observe or prepare — it removes
 * the call-through boilerplate and the risk of a replacement forgetting to
 * reproduce the original behavior.
 * <p>
 * Conventions (mirroring {@link DetourMethod}, minus the return value):
 * <ul>
 * <li>The advice method must be {@code public static void}.</li>
 * <li>Instance original {@code target.foo(a, b)}: advice signature is
 * {@code (Target self, A a, B b)}.</li>
 * <li>Static original {@code Target.foo(a, b)}: advice signature is
 * {@code (A a, B b)}.</li>
 * <li>Call sites are matched by method name and parameter types; the
 * original's return type does not participate (advice never touches it).</li>
 * <li>Constructors are not supported.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Before {
    /**
     * Original method name. If empty, the original name is assumed to be the
     * same as the advice method name.
     */
    String value() default "";
}
