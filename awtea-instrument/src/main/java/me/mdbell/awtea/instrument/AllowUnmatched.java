package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a detour may legitimately bind zero call sites, exempting it
 * from {@link DetourHacks#zeroMatchVerifier(boolean)}.
 * <p>
 * This is for <em>guard</em> detours, whose whole purpose is to fail loudly
 * <em>if</em> something ever calls the target — for those, matching nothing is
 * the success case, not a defect. It is not an off-switch: the detour is still
 * registered and still binds the moment a call site appears. Use
 * {@link DisableDetour} to actually turn a detour off.
 * <p>
 * Reach for this sparingly. Every unannotated detour that stops matching is a
 * hard build failure precisely so that a renamed or moved original cannot
 * silently stop being detoured; a chronically-unmatched detour left
 * unannotated erodes that signal by training readers to skim past the
 * "matched no call sites" line.
 *
 * @see DisableDetour
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AllowUnmatched {

    /** Why this detour is expected to bind nothing. Shown in the build log. */
    String value() default "";
}
