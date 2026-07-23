package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a static boolean method whose body must be exactly
 * {@code return false;}. When the enclosing class is registered with
 * {@link DetourHacks} for a build, the transformer rewrites the constant so
 * the method returns {@code true}.
 * <p>
 * This gives runtime code a truthful "was this detour class applied in this
 * build?" probe without hand-rolled did-my-detour-run-yet flags. Any build
 * that doesn't run the transformer over the class (a plain JVM launcher, or
 * the class simply not being registered) keeps the source behavior: not
 * applied.
 * <p>
 * The strict body convention exists because the rewrite edits the
 * javac-produced constant in place rather than synthesizing a replacement
 * program; {@link DetourHacks} fails the build if no {@code false} constant
 * is found to flip.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DetourApplied {
}
