package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a static String method whose body must be exactly
 * {@code return "<placeholder>";}. When a {@link BuildConstants} transformer
 * carrying a value for this key is registered for a build, the placeholder
 * constant is rewritten to that value.
 * <p>
 * The placeholder doubles as the honest untransformed default: a plain JVM
 * launch, or a build that registers no {@link BuildConstants}, returns it
 * unchanged — so pick a placeholder that reads as "not stamped" rather than
 * as data (e.g. {@code "unknown"}).
 * <p>
 * Values cannot be computed here: anything this method evaluates runs where
 * the app runs, not at build time. The build-JVM side supplies the value via
 * {@link BuildConstants#set(String, String)}. A reachable probe whose key has
 * no supplied value fails the build; a supplied value whose probe is never
 * reached is reported by {@link BuildConstants#unusedValueVerifier()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BuildConstant {
    /** Key the build supplies a value for — see {@link BuildConstants#set}. */
    String value();
}
