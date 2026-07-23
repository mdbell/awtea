package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a static boolean method whose body must be exactly
 * {@code return false;}. When a {@link BuildConstants} transformer carrying
 * {@code setFlag(key, true)} is registered for a build, the constant is
 * rewritten to {@code true}; with {@code setFlag(key, false)} the source
 * default stands.
 * <p>
 * The value of a flag over a runtime field is that branches gated on it —
 * {@code if (BuildFlags.dev()) ...} — see a compile-time constant, so the
 * optimizer can fold the condition and strip the guarded code from builds
 * where the flag is off. Use it for the residue that detour registration
 * can't gate: debug branches inside always-shipped methods, logging,
 * dev-only sanity checks.
 * <p>
 * Like {@link BuildConstant}: a reachable probe whose key was never supplied
 * fails the build (probe/plugin drift), a supplied key whose probe is never
 * reached is reported by {@link BuildConstants#unusedValueVerifier()}, and a
 * build that registers no {@link BuildConstants} at all (plain JVM launch)
 * keeps the source default of {@code false}. Flag keys share a namespace
 * with {@code @BuildConstant} string keys.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BuildFlag {
    /** Key the build supplies a value for — see {@link BuildConstants#setFlag}. */
    String value();
}
