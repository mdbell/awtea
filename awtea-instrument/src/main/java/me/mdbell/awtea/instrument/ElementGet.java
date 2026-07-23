package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Array-element read hook: {@link DetourHacks} inserts a call after every
 * element read of the array held by the named field on the class's
 * {@code @DetourReceiver} target, feeding the hook the index and the value
 * read; whatever the hook returns is what the reader sees.
 * <p>
 * Conventions:
 * <ul>
 * <li>The hook must be {@code public static}, returning the array's element
 * type, with signature {@code (T value)} extended to
 * {@code (Target self, int index, T value)} / {@code (int index, T value)} —
 * i.e. a leading receiver for instance fields, then the element index, then
 * the value, which must match the return type.</li>
 * <li>The declared element type is checked against the field's actual array
 * type at every matched site; a disagreement is a build error (type drift),
 * never a silent skip.</li>
 * </ul>
 * <b>Binding scope:</b> an element access binds when its array reference is
 * locally traceable, within the enclosing method, to a read of the named
 * field (directly or through simple assignments). References that arrive via
 * method parameters, merges of distinct control-flow paths, or other fields
 * are not traced — such sites are silently untouched, so pair element hooks
 * with the zero-match verifier and, where completeness matters, prefer
 * hooking the mutating code's seams instead.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ElementGet {
    /** Name of the array field on the {@code @DetourReceiver} target class. */
    String value();
}
