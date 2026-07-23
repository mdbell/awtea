package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Field-write hook: {@link DetourHacks} inserts a call before every write of
 * the named field on the class's {@code @DetourReceiver} target, feeding the
 * hook the value about to be written; whatever the hook returns is what
 * actually gets written. Return the parameter unchanged to observe without
 * mutating.
 * <p>
 * Signature and matching conventions are identical to {@link FieldGet}. Note
 * that compound updates ({@code x++}, {@code x |= ...}) trigger both a read
 * and a write, so a field hooked both ways sees both hooks fire.
 * <p>
 * For array-typed fields this hooks assignment of the array <em>reference</em>
 * ({@code target.arr = ...}); element stores ({@code target.arr[i] = ...})
 * read the field and mutate the array, so they fire {@link FieldGet}, not
 * this.
 * <p>
 * Writes inside {@code @NoDetours} classes are untouched, so a hook may
 * write its own target field without recursing.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FieldSet {
    /** Name of the field on the {@code @DetourReceiver} target class. */
    String value();
}
