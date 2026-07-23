package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Explicit off-switch for a detour. On a class, the whole detour class is
 * skipped at registration; on a method, just that mapping. Disabled detours
 * register nothing, produce no warnings, and are invisible to zero-match
 * verification — and a disabled class's {@link DetourApplied} probes keep
 * their source value of {@code false}, so runtime gates read correctly.
 * <p>
 * Prefer this over commenting out {@code @DetourReceiver}: a class registered
 * without a receiver is treated as a configuration error, precisely so that
 * an accidentally missing annotation cannot silently disable a detour.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DisableDetour {
}
