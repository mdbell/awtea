package me.mdbell.awtea.instrument.detour;

import me.mdbell.awtea.instrument.DetourMethod;
import me.mdbell.awtea.instrument.DetourReceiver;
import me.mdbell.awtea.instrument.FieldAccessor;
import me.mdbell.awtea.instrument.FieldSetter;
import me.mdbell.awtea.instrument.NoDetours;
import org.teavm.backend.javascript.spi.GeneratedBy;

import java.lang.reflect.Field;

/**
 * Example detour class demonstrating field accessor usage.
 * This is a reference implementation showing how to use @FieldAccessor and @FieldSetter
 * to access private fields in detour methods.
 * 
 * Note: This detour is not enabled by default (not in META-INF/awtea.detours).
 * It serves as documentation and a test case for the field accessor mechanism.
 */
@NoDetours
@DetourReceiver(target = Field.class)
public class FieldAccessorExample {

	/**
	 * Accessor for the private 'name' field in java.lang.reflect.Field.
	 */
	@FieldAccessor("name")
	@GeneratedBy(me.mdbell.awtea.instrument.FieldAccessorGenerator.class)
	private static native String getName(Field field);

	/**
	 * Setter for the private 'name' field in java.lang.reflect.Field.
	 */
	@FieldSetter("name")
	@GeneratedBy(me.mdbell.awtea.instrument.FieldAccessorGenerator.class)
	private static native void setName(Field field, String name);

	/**
	 * Example detour method that uses field accessors.
	 * This would replace Field.getName() if enabled.
	 */
	@DetourMethod("getName")
	public static String getNameDetour(Field field) {
		// Use generated field accessor instead of reflection
		String name = getName(field);
		return name != null ? name : "unknown";
	}
}
