package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a native method to be generated as a field setter by the FieldAccessorGenerator.
 * The annotated method must be:
 * - private
 * - static
 * - native
 * - Have exactly two parameters (the target instance and the new value)
 * - Return void
 * - Second parameter type must match the field type
 * 
 * Example:
 * <pre>
 * {@code
 * @DetourReceiver(target = SomeClass.class)
 * public class SomeClassDetours {
 *     @FieldSetter("privateField")
 *     private static native void setPrivateField(SomeClass instance, int value);
 *     
 *     @DetourMethod("someMethod")
 *     public static void someMethod(SomeClass self, int arg) {
 *         setPrivateField(self, arg + 42);
 *     }
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FieldSetter {
	/**
	 * The name of the field to set.
	 */
	String value();
}
