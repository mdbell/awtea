package me.mdbell.awtea.detour;

import java.lang.reflect.Field;

@NoDetours
public class FieldDetour {

	public static int getInt(Field field, Object obj) throws IllegalAccessException {
		Object value = field.get(obj);
		return ((Number)value).intValue();
	}

	public void setInt(Field field, Object obj, int value) throws IllegalAccessException {
		field.set(obj, value);
	}
}
