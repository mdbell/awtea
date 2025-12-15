package me.mdbell.awtea.instrument;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.model.MethodReference;

import java.lang.reflect.Method;

/**
 * TeaVM Generator that implements field accessor/setter methods annotated with
 * {@link FieldAccessor} or {@link FieldSetter}.
 * 
 * <p>This generator is invoked at compile-time by TeaVM when it encounters a native method
 * annotated with @FieldAccessor or @FieldSetter. It generates JavaScript code that directly
 * accesses the private fields of the target class.</p>
 * 
 * <p>Field accessors enable detour methods to read/write private fields without reflection,
 * which is essential for zero-copy WASM optimizations.</p>
 */
public class FieldAccessorGenerator implements Generator {
	
	private static final Logger log = LoggerFactory.getLogger(FieldAccessorGenerator.class);

	@Override
	public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) {
		// Note: Method lookup uses reflection. For better compilation performance with many
		// field accessors, consider implementing a caching mechanism in a future optimization.
		Method javaMethod = findJavaMethod(context.getClassLoader(), methodRef);
		
		FieldAccessor accessorAnn = javaMethod.getAnnotation(FieldAccessor.class);
		FieldSetter setterAnn = javaMethod.getAnnotation(FieldSetter.class);
		
		if (accessorAnn != null) {
			generateFieldGetter(context, writer, methodRef, javaMethod, accessorAnn);
		} else if (setterAnn != null) {
			generateFieldSetter(context, writer, methodRef, javaMethod, setterAnn);
		} else {
			throw new RuntimeException(
				"@FieldAccessor or @FieldSetter annotation missing on " + methodRef);
		}
	}

	/**
	 * Generates a field getter.
	 * 
	 * Input: static native ReturnType getField(TargetType instance)
	 * Output JS: return instance.fieldName;
	 */
	private void generateFieldGetter(GeneratorContext context, SourceWriter writer, 
									   MethodReference methodRef, Method javaMethod, 
									   FieldAccessor annotation) {
		String fieldName = annotation.value();
		
		// Validate method signature
		if (javaMethod.getParameterCount() != 1) {
			throw new RuntimeException(
				"@FieldAccessor method must have exactly one parameter (the target instance): " + methodRef);
		}
		
		if (javaMethod.getReturnType() == void.class) {
			throw new RuntimeException(
				"@FieldAccessor method must return the field value, not void: " + methodRef);
		}
		
		// Get the target class from @DetourReceiver annotation
		Class<?> detourClass = javaMethod.getDeclaringClass();
		DetourReceiver receiverAnn = detourClass.getAnnotation(DetourReceiver.class);
		if (receiverAnn == null) {
			throw new RuntimeException(
				"@FieldAccessor method's declaring class must be annotated with @DetourReceiver: " + methodRef);
		}
		
		log.debug("Generating field accessor for {}.{}", receiverAnn.target().getName(), fieldName);
		
		// Generate: return instance.fieldName;
		writer.append("return ");
		writer.append(context.getParameterName(0));
		writer.append(".").append(fieldName);
		writer.append(";").softNewLine();
	}

	/**
	 * Generates a field setter.
	 * 
	 * Input: static native void setField(TargetType instance, FieldType value)
	 * Output JS: instance.fieldName = value;
	 */
	private void generateFieldSetter(GeneratorContext context, SourceWriter writer, 
									   MethodReference methodRef, Method javaMethod, 
									   FieldSetter annotation) {
		String fieldName = annotation.value();
		
		// Validate method signature
		if (javaMethod.getParameterCount() != 2) {
			throw new RuntimeException(
				"@FieldSetter method must have exactly two parameters (target instance and new value): " + methodRef);
		}
		
		if (javaMethod.getReturnType() != void.class) {
			throw new RuntimeException(
				"@FieldSetter method must return void: " + methodRef);
		}
		
		// Get the target class from @DetourReceiver annotation
		Class<?> detourClass = javaMethod.getDeclaringClass();
		DetourReceiver receiverAnn = detourClass.getAnnotation(DetourReceiver.class);
		if (receiverAnn == null) {
			throw new RuntimeException(
				"@FieldSetter method's declaring class must be annotated with @DetourReceiver: " + methodRef);
		}
		
		log.debug("Generating field setter for {}.{}", receiverAnn.target().getName(), fieldName);
		
		// Generate: instance.fieldName = value;
		writer.append(context.getParameterName(0));
		writer.append(".").append(fieldName);
		writer.append(" = ");
		writer.append(context.getParameterName(1));
		writer.append(";").softNewLine();
	}

	/**
	 * Finds the Java method corresponding to a TeaVM method reference.
	 * Matches by name and parameter types.
	 */
	private Method findJavaMethod(ClassLoader cl, MethodReference ref) {
		String ownerName = ref.getClassName().replace('/', '.');
		try {
			Class<?> owner = Class.forName(ownerName, false, cl);
			
			// Get parameter types from descriptor
			int paramCount = ref.getDescriptor().parameterCount();
			Class<?>[] expectedParamTypes = new Class<?>[paramCount];
			
			for (int i = 0; i < paramCount; i++) {
				String paramTypeName = ref.getDescriptor().parameterType(i).toString();
				expectedParamTypes[i] = getClassForTypeName(paramTypeName, cl);
			}
			
			// Find matching method
			for (Method m : owner.getDeclaredMethods()) {
				if (!m.getName().equals(ref.getName())) {
					continue;
				}
				if (m.getParameterCount() != paramCount) {
					continue;
				}
				
				// Check parameter types match
				Class<?>[] actualParamTypes = m.getParameterTypes();
				boolean typesMatch = true;
				for (int i = 0; i < paramCount; i++) {
					if (!actualParamTypes[i].equals(expectedParamTypes[i])) {
						typesMatch = false;
						break;
					}
				}
				
				if (typesMatch) {
					return m;
				}
			}
			
			throw new RuntimeException("Method not found via reflection: " + ref);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Unable to load class for " + ref, e);
		}
	}
	
	/**
	 * Converts a TeaVM type name to a Java Class.
	 * Handles primitives and object types.
	 */
	private Class<?> getClassForTypeName(String typeName, ClassLoader cl) {
		// Handle primitive types
		switch (typeName) {
			case "I": return int.class;
			case "J": return long.class;
			case "F": return float.class;
			case "D": return double.class;
			case "Z": return boolean.class;
			case "B": return byte.class;
			case "C": return char.class;
			case "S": return short.class;
			case "V": return void.class;
		}
		
		// Handle object types (format: Ljava/lang/String;)
		if (typeName.startsWith("L") && typeName.endsWith(";")) {
			String className = typeName.substring(1, typeName.length() - 1).replace('/', '.');
			try {
				return Class.forName(className, false, cl);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Unable to load class: " + className, e);
			}
		}
		
		// Handle array types (format: [I, [[Ljava/lang/String;, etc.)
		if (typeName.startsWith("[")) {
			try {
				// For arrays, Class.forName expects the internal JVM format as-is
				// Don't replace slashes - array descriptors use the internal format
				return Class.forName(typeName, false, cl);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Unable to load array class: " + typeName, e);
			}
		}
		
		throw new RuntimeException("Unknown type name format: " + typeName);
	}
}
