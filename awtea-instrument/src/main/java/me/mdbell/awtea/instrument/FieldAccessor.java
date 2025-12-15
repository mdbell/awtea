package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a native method to be generated as a field getter by the FieldAccessorGenerator.
 * The annotated method must be:
 * - private
 * - static
 * - native
 * - Have exactly one parameter (the target instance)
 * - Return type must match the field type
 * 
 * Example:
 * <pre>
 * {@code
 * @DetourReceiver(target = SomeClass.class)
 * public class SomeClassDetours {
 *     @FieldAccessor("privateField")
 *     private static native int getPrivateField(SomeClass instance);
 *     
 *     @DetourMethod("someMethod")
 *     public static void someMethod(SomeClass self, int arg) {
 *         int value = getPrivateField(self);
 *         // use value...
 *     }
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FieldAccessor {
	/**
	 * The name of the field to access.
	 */
	String value();
}
