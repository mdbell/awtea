package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Field-read hook: {@link DetourHacks} inserts a call after every read of the
 * named field on the class's {@code @DetourReceiver} target, feeding the hook
 * the value that was read; whatever the hook returns is what the reader sees.
 * Return the parameter unchanged to observe without mutating.
 * <p>
 * Conventions (filter-shaped, like {@link Filter}):
 * <ul>
 * <li>The hook must be {@code public static}, returning the field's type,
 * with that same type as its trailing parameter.</li>
 * <li>Instance field {@code target.field}: hook signature is
 * {@code static T onGet(Target self, T value)}.</li>
 * <li>Static field {@code Target.field}: hook signature is
 * {@code static T onGet(T value)}.</li>
 * <li>Sites are matched by field name and instance-vs-static form; a site
 * whose field type disagrees with the hook's is a build error (type drift),
 * not a silent skip.</li>
 * </ul>
 * Reads inside {@code @NoDetours} classes are untouched, so a hook may read
 * its own target field without recursing.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FieldGet {
    /** Name of the field on the {@code @DetourReceiver} target class. */
    String value();
}
