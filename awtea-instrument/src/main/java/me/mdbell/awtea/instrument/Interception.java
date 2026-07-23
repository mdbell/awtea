package me.mdbell.awtea.instrument;

/**
 * Mutable carrier handed to a {@link Guard} method as its trailing parameter.
 * Leave it untouched and the original call proceeds; call {@link #cancel()}
 * or {@link #cancel(Object)} and the original is skipped, with the supplied
 * value substituted as the call's result.
 * <p>
 * Primitive-returning originals must be cancelled with the boxed value; the
 * call site unboxes via the typed accessors below. Cancelling a
 * primitive-returning original without a value fails fast at runtime rather
 * than silently substituting zero.
 * <p>
 * A fresh instance is allocated per guarded call, so guards may not retain it.
 */
public final class Interception {

    private boolean cancelled;
    private Object result;

    /**
     * Skip the original call. For originals returning a reference type the
     * substituted result is {@code null}; for void originals there is nothing
     * to substitute.
     */
    public void cancel() {
        cancelled = true;
        result = null;
    }

    /**
     * Skip the original call and substitute {@code result} as its value.
     * Pass the boxed form for primitive-returning originals.
     */
    public void cancel(Object result) {
        cancelled = true;
        this.result = result;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    // ----------------------------------------------------------------------
    // Typed accessors invoked by the generated call-site code. Not intended
    // for use from guard bodies.
    // ----------------------------------------------------------------------

    public Object getObject() {
        return result;
    }

    public boolean getBoolean() {
        return (Boolean) require();
    }

    public byte getByte() {
        return ((Number) require()).byteValue();
    }

    public short getShort() {
        return ((Number) require()).shortValue();
    }

    public char getChar() {
        return (Character) require();
    }

    public int getInt() {
        return ((Number) require()).intValue();
    }

    public long getLong() {
        return ((Number) require()).longValue();
    }

    public float getFloat() {
        return ((Number) require()).floatValue();
    }

    public double getDouble() {
        return ((Number) require()).doubleValue();
    }

    private Object require() {
        if (result == null) {
            throw new IllegalStateException(
                    "guard cancelled a primitive-returning method without supplying a result");
        }
        return result;
    }
}
