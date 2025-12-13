package me.mdbell.awtea.instrument.detour;

import me.mdbell.awtea.instrument.DetourMethod;
import me.mdbell.awtea.instrument.DetourReceiver;
import me.mdbell.awtea.instrument.NoDetours;

import java.lang.reflect.Field;

@NoDetours
@DetourReceiver(target = Field.class)
public class FieldDetour {

	@DetourMethod("getInt")
	public static int getInt(Field field, Object obj) throws IllegalAccessException {
		Object value = field.get(obj);
		return ((Number) value).intValue();
	}

	@DetourMethod("setInt")
	public static void setInt(Field field, Object obj, int value) throws IllegalAccessException {
		field.set(obj, value);
	}
}
