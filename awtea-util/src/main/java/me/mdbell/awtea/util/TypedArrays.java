package me.mdbell.awtea.util;

import org.teavm.classlib.PlatformDetector;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Float64Array;
import org.teavm.jso.typedarrays.Int16Array;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Int8Array;

/**
 * Platform-neutral conversions between Java arrays and JS typed arrays.
 *
 * On the JS backend a Java array is backed by a typed array, so TeaVM's
 * {@code fromJavaArray}/{@code toJavaArray} return zero-copy views — but they
 * are {@code @JSByRef}, which the wasm-gc backend rejects at build time (Java
 * arrays can only cross the JS boundary by copy there). These helpers select
 * the zero-copy view on JS and the copying variant on wasm-gc; the
 * {@link PlatformDetector} branch is folded to a constant during compilation,
 * so each backend only ever sees its own path.
 *
 * Callers must not rely on the result aliasing the source: on wasm-gc it
 * never does. Code that needs a live shared view (pixel stores, data buffers)
 * needs per-platform structure instead — see docs/wasm-port-plan.md in the
 * client repo (PixelBuffer / Lane 2).
 */
public final class TypedArrays {

    private TypedArrays() {
    }

    public static Int8Array from(byte[] arr) {
        return PlatformDetector.isWebAssemblyGC() ? Int8Array.copyFromJavaArray(arr) : Int8Array.fromJavaArray(arr);
    }

    public static Int16Array from(short[] arr) {
        return PlatformDetector.isWebAssemblyGC() ? Int16Array.copyFromJavaArray(arr) : Int16Array.fromJavaArray(arr);
    }

    public static Int32Array from(int[] arr) {
        return PlatformDetector.isWebAssemblyGC() ? Int32Array.copyFromJavaArray(arr) : Int32Array.fromJavaArray(arr);
    }

    public static Float32Array from(float[] arr) {
        return PlatformDetector.isWebAssemblyGC() ? Float32Array.copyFromJavaArray(arr)
                : Float32Array.fromJavaArray(arr);
    }

    public static Float64Array from(double[] arr) {
        return PlatformDetector.isWebAssemblyGC() ? Float64Array.copyFromJavaArray(arr)
                : Float64Array.fromJavaArray(arr);
    }

    public static byte[] toJavaArray(Int8Array arr) {
        return PlatformDetector.isWebAssemblyGC() ? arr.copyToJavaArray() : arr.toJavaArray();
    }

    public static short[] toJavaArray(Int16Array arr) {
        return PlatformDetector.isWebAssemblyGC() ? arr.copyToJavaArray() : arr.toJavaArray();
    }

    public static int[] toJavaArray(Int32Array arr) {
        return PlatformDetector.isWebAssemblyGC() ? arr.copyToJavaArray() : arr.toJavaArray();
    }

    public static float[] toJavaArray(Float32Array arr) {
        return PlatformDetector.isWebAssemblyGC() ? arr.copyToJavaArray() : arr.toJavaArray();
    }

    public static double[] toJavaArray(Float64Array arr) {
        return PlatformDetector.isWebAssemblyGC() ? arr.copyToJavaArray() : arr.toJavaArray();
    }
}
