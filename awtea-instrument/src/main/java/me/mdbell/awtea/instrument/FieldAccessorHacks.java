package me.mdbell.awtea.instrument;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.model.*;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.model.instructions.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * ClassHolderTransformer that implements field accessor/setter methods by injecting
 * public getter/setter methods into target classes and replacing native method calls
 * with calls to these injected methods.
 * 
 * <p>This transformer operates on bytecode at compile-time, similar to DetourHacks,
 * by:</p>
 * <ol>
 *   <li>Scanning for methods annotated with @FieldAccessor or @FieldSetter</li>
 *   <li>Injecting corresponding public getter/setter methods into the target class</li>
 *   <li>Replacing calls to the native accessor methods with calls to injected methods</li>
 * </ol>
 * 
 * <p>This approach ensures field accessors work correctly with TeaVM's compilation model.</p>
 */
public class FieldAccessorHacks implements ClassHolderTransformer {
	
	private static final Logger log = LoggerFactory.getLogger(FieldAccessorHacks.class);
	
	/**
	 * Registry of field accessors to inject, keyed by target class name.
	 */
	private final Map<String, List<FieldAccessorInfo>> accessorsToInject = new HashMap<>();
	
	/**
	 * Registry of field accessor method calls to replace, keyed by accessor method reference.
	 */
	private final Map<MethodReference, FieldAccessorInfo> accessorMethods = new HashMap<>();
	
	/**
	 * Information about a field accessor that needs to be injected.
	 */
	private static class FieldAccessorInfo {
		final String targetClassName;
		final String fieldName;
		final ValueType fieldType;
		final boolean isGetter; // true for getter, false for setter
		final MethodReference accessorMethod; // The native method to replace
		final String injectedMethodName; // Name of the injected method in target class
		
		FieldAccessorInfo(String targetClassName, String fieldName, ValueType fieldType,
						  boolean isGetter, MethodReference accessorMethod, String injectedMethodName) {
			this.targetClassName = targetClassName;
			this.fieldName = fieldName;
			this.fieldType = fieldType;
			this.isGetter = isGetter;
			this.accessorMethod = accessorMethod;
			this.injectedMethodName = injectedMethodName;
		}
	}
	
	public FieldAccessorHacks(Collection<Class<?>> detourClasses) {
		buildAccessorRegistry(detourClasses);
	}
	
	/**
	 * Scans detour classes for @FieldAccessor and @FieldSetter annotations and builds
	 * the registry of accessors to inject.
	 */
	private void buildAccessorRegistry(Collection<Class<?>> detourClasses) {
		for (Class<?> detourClass : detourClasses) {
			DetourReceiver receiver = detourClass.getAnnotation(DetourReceiver.class);
			if (receiver == null) {
				continue;
			}
			
			Class<?> targetClass = receiver.target();
			String targetClassName = targetClass.getName();
			
			for (Method method : detourClass.getDeclaredMethods()) {
				FieldAccessor accessorAnn = method.getAnnotation(FieldAccessor.class);
				FieldSetter setterAnn = method.getAnnotation(FieldSetter.class);
				
				if (accessorAnn == null && setterAnn == null) {
					continue;
				}
				
				// Validate method is native and static
				if (!Modifier.isNative(method.getModifiers()) || !Modifier.isStatic(method.getModifiers())) {
					log.warn("Field accessor method must be native and static: {}", method);
					continue;
				}
				
				String fieldName;
				boolean isGetter;
				ValueType fieldType;
				
				if (accessorAnn != null) {
					fieldName = accessorAnn.value();
					isGetter = true;
					fieldType = getValueType(method.getReturnType());
				} else {
					fieldName = setterAnn.value();
					isGetter = false;
					// For setter, field type is the second parameter
					if (method.getParameterCount() != 2) {
						log.warn("Field setter must have exactly 2 parameters: {}", method);
						continue;
					}
					fieldType = getValueType(method.getParameterTypes()[1]);
				}
				
				// Generate unique name for injected method
				String injectedMethodName = "$awtea$" + (isGetter ? "get" : "set") + "$" + fieldName;
				
				// Build method reference for the accessor method
				MethodReference accessorMethodRef = buildMethodReference(detourClass, method);
				
				FieldAccessorInfo info = new FieldAccessorInfo(
					targetClassName,
					fieldName,
					fieldType,
					isGetter,
					accessorMethodRef,
					injectedMethodName
				);
				
				accessorsToInject.computeIfAbsent(targetClassName, k -> new ArrayList<>()).add(info);
				accessorMethods.put(accessorMethodRef, info);
				
				log.debug("Registered field accessor: {} for {}.{}", 
					isGetter ? "getter" : "setter", targetClassName, fieldName);
			}
		}
	}
	
	private MethodReference buildMethodReference(Class<?> declaringClass, Method method) {
		Class<?>[] paramTypes = method.getParameterTypes();
		Class<?> returnType = method.getReturnType();
		
		ValueType[] signature = new ValueType[paramTypes.length + 1];
		for (int i = 0; i < paramTypes.length; i++) {
			signature[i] = getValueType(paramTypes[i]);
		}
		signature[paramTypes.length] = getValueType(returnType);
		
		return new MethodReference(
			declaringClass.getName(),
			new MethodDescriptor(method.getName(), signature)
		);
	}
	
	private ValueType getValueType(Class<?> clazz) {
		if (clazz == void.class) return ValueType.VOID;
		if (clazz == boolean.class) return ValueType.BOOLEAN;
		if (clazz == byte.class) return ValueType.BYTE;
		if (clazz == short.class) return ValueType.SHORT;
		if (clazz == char.class) return ValueType.CHARACTER;
		if (clazz == int.class) return ValueType.INTEGER;
		if (clazz == long.class) return ValueType.LONG;
		if (clazz == float.class) return ValueType.FLOAT;
		if (clazz == double.class) return ValueType.DOUBLE;
		if (clazz.isArray()) {
			return ValueType.arrayOf(getValueType(clazz.getComponentType()));
		}
		return ValueType.object(clazz.getName());
	}
	
	@Override
	public void transformClass(ClassHolder classHolder, ClassHolderTransformerContext context) {
		String className = classHolder.getName();
		
		try {
			// Step 1: Inject getter/setter methods into target classes
			List<FieldAccessorInfo> accessors = accessorsToInject.get(className);
			if (accessors != null) {
				for (FieldAccessorInfo info : accessors) {
					injectAccessorMethod(classHolder, info, context.getHierarchy());
				}
			}
			
			// Step 2: Replace calls to native accessor methods with calls to injected methods
			for (MethodHolder method : classHolder.getMethods()) {
				if (method.hasProgram()) {
					replaceAccessorCalls(method);
				}
			}
		} catch (Exception e) {
			log.error("Error transforming class {}: {}", className, e.getMessage(), e);
			throw new RuntimeException("FieldAccessorHacks failed for " + className, e);
		}
	}
	
	/**
	 * Injects a public getter or setter method into the target class.
	 */
	private void injectAccessorMethod(ClassHolder classHolder, FieldAccessorInfo info, ClassHierarchy hierarchy) {
		// Check if method already exists
		if (classHolder.getMethod(new MethodDescriptor(info.injectedMethodName, 
				buildInjectedMethodSignature(info))) != null) {
			return; // Already injected
		}
		
		// Check if the field exists in this class
		FieldHolder fieldHolder = classHolder.getField(info.fieldName);
		if (fieldHolder == null) {
			log.warn("Field {} not found in class {}, skipping accessor injection", 
				info.fieldName, classHolder.getName());
			return;
		}
		
		MethodHolder injectedMethod;
		
		if (info.isGetter) {
			// Create: public FieldType $awtea$get$fieldName() { return this.fieldName; }
			ValueType[] signature = new ValueType[] { info.fieldType };
			injectedMethod = new MethodHolder(new MethodDescriptor(info.injectedMethodName, signature));
			injectedMethod.setLevel(AccessLevel.PUBLIC);
			
			Program program = new Program();
			injectedMethod.setProgram(program);
			
			// Pre-create variable 0 for 'this' (instance methods automatically have this)
			Variable thisVar = program.createVariable();
			thisVar.setDebugName("this");
			
			ProgramEmitter pe = ProgramEmitter.create(program, hierarchy);
			BasicBlock block = program.createBasicBlock();
			pe.enter(block);
			
			// Get field reference
			FieldReference fieldRef = new FieldReference(classHolder.getName(), info.fieldName);
			
			// Load this.fieldName and return it
			ValueEmitter fieldValue = pe.getField(fieldRef, info.fieldType);
			fieldValue.returnValue();
			
		} else {
			// Create: public void $awtea$set$fieldName(FieldType value) { this.fieldName = value; }
			ValueType[] signature = new ValueType[] { info.fieldType, ValueType.VOID };
			injectedMethod = new MethodHolder(new MethodDescriptor(info.injectedMethodName, signature));
			injectedMethod.setLevel(AccessLevel.PUBLIC);
			
			Program program = new Program();
			injectedMethod.setProgram(program);
			
			// Pre-create variable 0 for 'this' (instance methods automatically have this)
			Variable thisVar = program.createVariable();
			thisVar.setDebugName("this");
			
			// Pre-create variable 1 for the value parameter
			Variable valueVar = program.createVariable();
			valueVar.setDebugName("value");
			
			ProgramEmitter pe = ProgramEmitter.create(program, hierarchy);
			BasicBlock block = program.createBasicBlock();
			pe.enter(block);
			
			// Wrap variable 1 as a ValueEmitter
			ValueEmitter valueParam = pe.var(valueVar, info.fieldType);
			
			// Set this.fieldName = value
			pe.setField(classHolder.getName(), info.fieldName, valueParam);
			
			// Return void
			pe.exit();
		}
		
		classHolder.addMethod(injectedMethod);
		log.debug("Injected {} method into {}: {}", 
			info.isGetter ? "getter" : "setter", classHolder.getName(), info.injectedMethodName);
	}
	
	private ValueType[] buildInjectedMethodSignature(FieldAccessorInfo info) {
		if (info.isGetter) {
			return new ValueType[] { info.fieldType };
		} else {
			return new ValueType[] { info.fieldType, ValueType.VOID };
		}
	}
	
	/**
	 * Replaces calls to native accessor methods with calls to injected methods.
	 */
	private void replaceAccessorCalls(MethodHolder method) {
		Program program = method.getProgram();
		
		for (BasicBlock block : program.getBasicBlocks()) {
			for (Instruction insn : block) {
				if (insn instanceof InvokeInstruction) {
					InvokeInstruction invoke = (InvokeInstruction) insn;
					MethodReference methodRef = invoke.getMethod();
					
					FieldAccessorInfo info = accessorMethods.get(methodRef);
					if (info != null) {
						replaceAccessorCall(invoke, info);
					}
				}
			}
		}
	}
	
	/**
	 * Replaces a single accessor call with a call to the injected method.
	 */
	private void replaceAccessorCall(InvokeInstruction invoke, FieldAccessorInfo info) {
		// Original call: AccessorClass.getField(targetInstance) or AccessorClass.setField(targetInstance, value)
		// New call: targetInstance.$awtea$get$field() or targetInstance.$awtea$set$field(value)
		
		List<? extends Variable> argsList = invoke.getArguments();
		if (argsList.isEmpty()) {
			log.warn("Field accessor call has no arguments: {}", invoke.getMethod());
			return;
		}
		
		// First argument is always the target instance
		Variable targetInstance = argsList.get(0);
		
		// Build new method reference to injected method
		MethodReference injectedMethodRef = new MethodReference(
			info.targetClassName,
			new MethodDescriptor(info.injectedMethodName, buildInjectedMethodSignature(info))
		);
		
		// Update the invoke instruction
		invoke.setMethod(injectedMethodRef);
		invoke.setInstance(targetInstance);
		invoke.setType(InvocationType.VIRTUAL);
		
		if (info.isGetter) {
			// Getter: no other arguments needed
			invoke.setArguments(new Variable[0]);
		} else {
			// Setter: pass the value (second argument)
			if (argsList.size() < 2) {
				log.warn("Field setter call has insufficient arguments: {}", invoke.getMethod());
				return;
			}
			invoke.setArguments(new Variable[] { argsList.get(1) });
		}
		
		log.debug("Replaced accessor call to {} with injected method {}", 
			info.accessorMethod, injectedMethodRef);
	}
	
	/**
	 * Factory method to create FieldAccessorHacks from a resource file.
	 */
	public static FieldAccessorHacks fromResource(String resourcePath) {
		// Reuse DetourHacks logic to load classes from resource
		// For now, we'll scan all detour classes
		List<Class<?>> classes = new ArrayList<>();
		try {
			ClassLoader cl = FieldAccessorHacks.class.getClassLoader();
			java.util.Enumeration<java.net.URL> resources = cl.getResources(resourcePath);
			List<java.net.URL> urls = Collections.list(resources);
			for (java.net.URL resourceUrl : urls) {
				try (java.io.InputStream is = resourceUrl.openStream();
					 java.io.BufferedReader reader = new java.io.BufferedReader(
							 new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) {
						line = stripComment(line).trim();
						if (line.isEmpty()) {
							continue;
						}
						try {
							Class<?> cls = Class.forName(line, true, cl);
							classes.add(cls);
						} catch (ClassNotFoundException e) {
							log.warn("Failed to load class for field accessors: {}", line);
						}
					}
				}
			}
		} catch (java.io.IOException e) {
			log.error("Failed to load field accessor classes from resource: {}", resourcePath, e);
		}
		return new FieldAccessorHacks(classes);
	}
	
	private static String stripComment(String line) {
		int idx = line.indexOf('#');
		return (idx >= 0) ? line.substring(0, idx) : line;
	}
}
