package me.mdbell.awtea.transform;

import org.teavm.model.*;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.util.ModelUtils;

import java.util.Arrays;

public class AwtPolyfillTransformer implements ClassHolderTransformer {

	private static final String CLASSNAME_PREFIX = "T";
	private static final String TEAVM_PREFIX = "org.teavm.classlib";

	private static final String AWTEA_PREFIX = "me.mdbell.awtea.polyfill";

	public String getPolyfillClassName(String className){
		String[] classParts = className.split("\\.");
		classParts[classParts.length - 1] = CLASSNAME_PREFIX + classParts[classParts.length - 1];
		return String.join(".", classParts);
	}

	private String getPrefixedName(String className) {
		String[] parts = className.split("\\.");
		parts[parts.length - 1] = CLASSNAME_PREFIX + parts[parts.length - 1];
		return String.join(".", parts);
	}

	private String getUnprefixedName(String className) {
		String[] parts = className.split("\\.");
		parts[parts.length - 1] = parts[parts.length - 1].substring(CLASSNAME_PREFIX.length());
		return String.join(".", parts);
	}

	public boolean classExistsOnClassPath(String prefix, String className){
		String[] prefixParts = prefix.split("\\.");
		String[] classParts = className.split("\\.");

		classParts[classParts.length - 1] = CLASSNAME_PREFIX + classParts[classParts.length - 1];

		String fullName = String.join("/", prefixParts) + "/" + String.join("/", classParts) + ".class";

		// Check if the class file exists in the classpath
		return getClass().getClassLoader().getResource(fullName) != null;
	}

	@Override
	public void transformClass(ClassHolder cls, ClassHolderTransformerContext ctx) {

		if(!classExistsOnClassPath(TEAVM_PREFIX, cls.getName())) {
			return;
		}

		if (!classExistsOnClassPath(AWTEA_PREFIX, cls.getName())) {
			return;
		}

		System.out.println("Applying AWT polyfill for class: " + cls.getName());

		String classImpl = AWTEA_PREFIX + '.' + getPrefixedName(cls.getName());

		ClassReaderSource source = ctx.getHierarchy().getClassSource();

		ClassReader polyfill = source.get(classImpl);

		if (polyfill == null) {
			throw new RuntimeException("Polyfill class not found: " + classImpl);
		}

		// Remove existing fields/methods on java.awt.Point
		for (FieldHolder f : cls.getFields().toArray(new FieldHolder[0])) {
			cls.removeField(f);
		}
		for (MethodHolder m : cls.getMethods().toArray(new MethodHolder[0])) {
			cls.removeMethod(m);
		}

		// Copy fields/methods from your impl into java.awt.Point
		for (FieldReader f : polyfill.getFields()) {
			cls.addField(ModelUtils.copyField(f));
		}
		for (MethodReader m : polyfill.getMethods()) {
			MethodHolder m1 = ModelUtils.copyMethod(m);

			transformMethod(m1);

			cls.addMethod(m1);
		}
	}

	private void transformMethod(MethodHolder method) {
		if (!method.hasProgram()) {
			return;
		}

		method.getProgram().getBasicBlocks().forEach(this::transformBlock);
	}

	private void transformBlock(BasicBlock instructions) {
		instructions.forEach(instruction -> {
			if (instruction instanceof InvokeInstruction) {
				transformInvoke((InvokeInstruction) instruction);
			}else if (instruction instanceof GetFieldInstruction) {
				GetFieldInstruction getField = (GetFieldInstruction) instruction;
				String className = getField.getField().getClassName();

				if(className.startsWith(AWTEA_PREFIX)) {
					FieldReference fr = getField.getField();
					String newClassName = className.substring(AWTEA_PREFIX.length() + 1);
					newClassName = getUnprefixedName(newClassName);
					getField.setField(new FieldReference(newClassName, fr.getFieldName()));
				}
			}else if (instruction instanceof PutFieldInstruction) {
				PutFieldInstruction putField = (PutFieldInstruction) instruction;
				String className = putField.getField().getClassName();

				if(className.startsWith(AWTEA_PREFIX)) {
					FieldReference fr = putField.getField();
					String newClassName = className.substring(AWTEA_PREFIX.length() + 1);
					newClassName = getUnprefixedName(newClassName);
					putField.setField(new FieldReference(newClassName, fr.getFieldName()));
				}
			}else if (instruction instanceof ConstructInstruction) {
				ConstructInstruction construct = (ConstructInstruction) instruction;
				String className = construct.getType();

				if(className.startsWith(AWTEA_PREFIX)) {
					String newClassName = className.substring(AWTEA_PREFIX.length() + 1);
					newClassName = getUnprefixedName(newClassName);
					construct.setType(newClassName);
				}
			}
		});
	}

	private void transformInvoke(InvokeInstruction invoke) {
		MethodReference ref = invoke.getMethod();
		String className = ref.getClassName();

		if(className.startsWith(AWTEA_PREFIX)) {
			String newClassName = className.substring(AWTEA_PREFIX.length() + 1);
			newClassName = getUnprefixedName(newClassName);
			MethodDescriptor desc = transformDescriptor(ref.getDescriptor());
			invoke.setMethod(new MethodReference((newClassName
				), desc));
		}
	}

	private MethodDescriptor transformDescriptor(MethodDescriptor descriptor) {
		ValueType[] signature = Arrays.stream(descriptor.getSignature()).map(this::transformType).toArray(ValueType[]::new);

		return new MethodDescriptor(descriptor.getName(), signature);
	}

	private ValueType transformType(ValueType type) {
		if (type instanceof ValueType.Object) {
			String className = ((ValueType.Object) type).getClassName();
			if (className.startsWith(AWTEA_PREFIX)) {
				String newClassName = className.substring(AWTEA_PREFIX.length() + 1);
				newClassName = getUnprefixedName(newClassName);
				return new ValueType.Object(newClassName);
			}
		}
		return type;
	}
}
