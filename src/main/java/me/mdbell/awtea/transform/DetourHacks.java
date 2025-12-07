package me.mdbell.awtea.transform;

import me.mdbell.awtea.detour.FieldDetour;
import me.mdbell.awtea.detour.NoDetours;
import me.mdbell.awtea.detour.RandomAccessFileDetour;
import me.mdbell.awtea.detour.SystemDetour;
import org.teavm.model.*;
import org.teavm.model.instructions.InvokeInstruction;

import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetourHacks implements ClassHolderTransformer {

	static class MethodDetour {
		public final MethodDescriptor desc;
		public final Class<?> newClass;
		public final String newMethodName;

		public MethodDetour(MethodDescriptor descriptor, Class<?> newClass) {
			this(descriptor, newClass, descriptor.getName());
		}

		public MethodDetour(MethodDescriptor descriptor, Class<?> newClass, String newMethodName) {
			this.desc = descriptor;
			this.newClass = newClass;
			this.newMethodName = newMethodName;
		}
	}

	Map<String, MethodDetour[]> detours = new HashMap<>() {{
		put(System.class.getName(), new MethodDetour[]{
			new MethodDetour(MethodDescriptor.parse("exit(I)V"), SystemDetour.class)
		});
		put(Field.class.getName(), new MethodDetour[]{
			new MethodDetour(MethodDescriptor.parse("getInt(Ljava/lang/Object;)I"), FieldDetour.class),
			new MethodDetour(MethodDescriptor.parse("setInt(Ljava/lang/Object;I)V"), FieldDetour.class),
		});
		put(RandomAccessFile.class.getName(), new MethodDetour[]{
			new MethodDetour(MethodDescriptor.parse("<init>(Ljava/io/File;Ljava/lang/String;)V"),
				RandomAccessFileDetour.class, "open"),
		});
		put(Thread.class.getName(), new MethodDetour[]{
			new MethodDetour(MethodDescriptor.parse("<init>(Ljava/lang/Runnable;Ljava/lang/String;)V"),
				me.mdbell.awtea.detour.ThreadDetour.class, "init"),
			new MethodDetour(MethodDescriptor.parse("<init>(Ljava/lang/Runnable;)V"),
				me.mdbell.awtea.detour.ThreadDetour.class, "init"),
			new MethodDetour(MethodDescriptor.parse("<init>(Ljava/lang/String;)V"),
				me.mdbell.awtea.detour.ThreadDetour.class, "init"),
			new MethodDetour(MethodDescriptor.parse("<init>()V"),
				me.mdbell.awtea.detour.ThreadDetour.class, "init"),
			new MethodDetour(MethodDescriptor.parse("start()V"),
				me.mdbell.awtea.detour.ThreadDetour.class),
			new MethodDetour(MethodDescriptor.parse("sleep(J)V"),
				me.mdbell.awtea.detour.ThreadDetour.class),
			new MethodDetour(MethodDescriptor.parse("setDaemon(Z)V"),
				me.mdbell.awtea.detour.ThreadDetour.class),
			new MethodDetour(MethodDescriptor.parse("setPriority(I)V"),
				me.mdbell.awtea.detour.ThreadDetour.class),
		});
	}};

	@Override
	public void transformClass(ClassHolder classHolder, ClassHolderTransformerContext classHolderTransformerContext) {
		if (classHolder.getAnnotations().get(NoDetours.class.getName()) != null) {
			return;
		}
		classHolder.getMethods().forEach(this::transformMethod);
	}

	private void transformMethod(MethodHolder method) {
		if (!method.hasProgram()) {
			return;
		}

		method.getProgram().getBasicBlocks().forEach(block -> transformBlock(method, block));
	}

	private void transformBlock(MethodHolder method, BasicBlock instructions) {
		instructions.forEach(instruction -> {
			if (instruction instanceof InvokeInstruction) {
				transformInvoke(method, (InvokeInstruction) instruction);
			}
		});
	}

	private void transformInvoke(MethodHolder method, InvokeInstruction invoke) {
		MethodReference ref = invoke.getMethod();
		MethodDetour[] detours = this.detours.get(ref.getClassName());

		if (detours == null) {
			return;
		}

		for (MethodDetour detour : detours) {
			MethodDescriptor desc = detour.desc;
			if (ref.getDescriptor().equals(desc)) {
				Variable thisVar = invoke.getInstance();

				if (ref.getName().equals("<init>")) {
					if (method.getOwnerName().equals(invoke.getMethod().getClassName()) && thisVar.getIndex() == 0) {
						// skip constructor detours for constructors of the same class
						continue;
					}
					// constructor detour - need to convert to a static method call
					thisVar = invoke.getInstance();
					invoke.setReceiver(thisVar);
					invoke.setInstance(null);
					ValueType[] signature = desc.getSignature();
					signature[signature.length - 1] = ValueType.object(ref.getClassName());
					MethodDescriptor newDesc = new MethodDescriptor(detour.newMethodName, signature);
					invoke.setMethod(new MethodReference(detour.newClass.getName(), newDesc));
					continue;
				}

				MethodDescriptor descriptor = detour.desc;
				if (thisVar != null) {
					// need to pass the instance as the first argument for non-static methods
					List<Variable> newArgs = new ArrayList<>();
					newArgs.add(thisVar);
					newArgs.addAll(invoke.getArguments());
					invoke.setArguments(newArgs.toArray(new Variable[0]));
					invoke.setInstance(null);
					ValueType[] signature = descriptor.getSignature();
					ValueType[] newSignature = new ValueType[signature.length + 1];
					newSignature[0] = ValueType.object(invoke.getMethod().getClassName());
					System.arraycopy(signature, 0, newSignature, 1, signature.length);
					descriptor = new MethodDescriptor(descriptor.getName(), newSignature);
				}
				invoke.setMethod(new MethodReference(detour.newClass.getName(), descriptor));
			}
		}
	}
}
