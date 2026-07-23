package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Array-element write hook: {@link DetourHacks} inserts a call before every
 * element store into the array held by the named field on the class's
 * {@code @DetourReceiver} target, feeding the hook the index and the value
 * about to be stored; whatever the hook returns is what actually gets
 * stored.
 * <p>
 * Signature, matching and — importantly — the local-traceability binding
 * scope are identical to {@link ElementGet}: only element accesses whose
 * array reference is traceable within the enclosing method to a read of the
 * named field are hooked. This is the element-level complement of
 * {@link FieldSet}, which sees assignment of the array reference itself.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ElementSet {
    /** Name of the array field on the {@code @DetourReceiver} target class. */
    String value();
}
