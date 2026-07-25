package me.mdbell.awtea.util;

import org.teavm.classlib.PlatformDetector;

/**
 * Platform checks that are safe to call from package-mapped classlib classes.
 *
 * TeaVM's {@code @PlatformMarker} rewrite (which folds
 * {@link PlatformDetector} calls to compile-time constants) does not apply
 * inside classes renamed via {@code mapPackageHierarchy} — there the call
 * survives to runtime and evaluates false on every backend. Same root cause
 * family as {@code @JSBody} not being processed in mapped classes (see
 * BrowserToolkitSupport). This class is NOT package-mapped, so the constant
 * folds here and mapped callers get the right answer via a normal call.
 */
public final class PlatformSupport {

    private PlatformSupport() {
    }

    public static boolean isWebAssemblyGC() {
        return PlatformDetector.isWebAssemblyGC();
    }
}
