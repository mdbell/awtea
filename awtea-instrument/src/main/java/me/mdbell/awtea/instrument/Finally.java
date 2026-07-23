package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Advice that runs on both exits of the original call: {@link DetourHacks}
 * wraps every matched call site in a synthesized catch-all, so the hook runs
 * after normal completion <em>and</em> when the original throws — in the
 * latter case the original exception is rethrown afterwards, still visible to
 * the method's own enclosing handlers. This is the piece that lets
 * save-and-restore detours stop being full replacements.
 * <p>
 * Signature and matching conventions are identical to {@link Before}/
 * {@link After}. Semantics mirror a Java {@code finally} block, including the
 * sharp edge: if the hook itself throws on the exception path, its exception
 * supersedes the original one.
 * <p>
 * Composition: {@link Before}/{@link After} advice and {@link Filter}s on the
 * same call site end up <em>inside</em> the synthesized try, so their
 * exceptions also trigger the hook; a replacement ({@link DetourMethod})
 * composes too — the try wraps the replacement call. A {@link Guard} on the
 * same site, or a second {@code @Finally}, has no defined composition and
 * fails the build.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Finally {
    /**
     * Original method name. If empty, the original name is assumed to be the
     * same as the advice method name.
     */
    String value() default "";
}
