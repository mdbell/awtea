package me.mdbell.awtea.classlib.java.lang.reflect;

import org.teavm.classlib.impl.reflection.FieldReader;
import org.teavm.classlib.impl.reflection.FieldWriter;

public class TField {

	public TField(Class<?> declaringClass, String name, int modifiers, int accessLevel,
				  Class<?> type, FieldReader reader, FieldWriter writer) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

    public int getInt(Object obj) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void setInt(Object obj, int value) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public int getModifiers() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public String getName() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
