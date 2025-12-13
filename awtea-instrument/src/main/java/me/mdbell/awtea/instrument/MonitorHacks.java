package me.mdbell.awtea.instrument;

import lombok.AllArgsConstructor;
import me.mdbell.awtea.monitor.OperationsMonitor;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.model.*;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.util.ModelUtils;
import org.teavm.model.util.ProgramUtils;

import java.util.ArrayList;
import java.util.List;

public class MonitorHacks implements ClassHolderTransformer {

	private static final Logger log = LoggerFactory.getLogger(MonitorHacks.class);

	private static final String MONITOR_CLASS = OperationsMonitor.class.getName();
	private static final String MONITOR_FIELD = "$awtea$monitor";

	private static final String METHOD_IMPL_SUFFIX = "$impl";

	private static final MethodReference onEnterRef = new MethodReference(MONITOR_CLASS,
		new MethodDescriptor("onEnter",
			ValueType.object("java.lang.Object"),
			ValueType.object("java.lang.String"),
			ValueType.VOID
		)
	);

	private static final MethodReference onLeaveRef = new MethodReference(MONITOR_CLASS,
		new MethodDescriptor("onLeave",
			ValueType.object("java.lang.Object"),
			ValueType.object("java.lang.String"),
			ValueType.VOID
		)
	);

	private static final MethodReference onThrownRef = new MethodReference(MONITOR_CLASS,
		new MethodDescriptor("onThrown",
			ValueType.object("java.lang.Object"),
			ValueType.object("java.lang.String"),
			ValueType.object("java.lang.Throwable"),
			ValueType.VOID
		)
	);

	@AllArgsConstructor
	static class MonitorMethod {
		MethodHolder original;
		String name;
	}

	@Override
	public void transformClass(ClassHolder cls, ClassHolderTransformerContext ctx) {
		List<MonitorMethod> monitored = new ArrayList<>();

		if (cls.getAnnotations().get(Monitored.Disabled.class.getName()) != null) {
			log.debug("Skipping monitoring for disabled class: {}", cls.getName());
			return;
		}

		AnnotationHolder referencePolicy = cls.getAnnotations().get(MonitorReferencePolicy.class.getName());
		MonitorReferencePolicy.Policy policy = referencePolicy != null
			? MonitorReferencePolicy.Policy.valueOf(referencePolicy.getValue("value").getEnumValue().getFieldName())
			: MonitorReferencePolicy.Policy.WEAK;

		boolean allMethods = cls.getAnnotations().get(Monitored.AllMethods.class.getName()) != null;

		for (MethodHolder method : cls.getMethods()) {
			String name = method.getName();
			if (name.equals("<init>") || name.equals("<clinit>")) {
				continue; // skip constructors and class initializers - if need be, they can be monitored via detours
			}
			if (allMethods || method.getAnnotations().get(Monitored.class.getName()) != null) {
				monitored.add(new MonitorMethod(method, resolveMonitorName(cls, method)));
			}
		}

		if (monitored.isEmpty()) {
			return;
		}

		ensureMonitorFieldAndClinit(cls, ctx.getHierarchy(), policy);

		for (MonitorMethod original : monitored) {
			wrapWithMonitor(cls, original, ctx.getHierarchy());
		}
	}

	private String resolveMonitorName(ClassHolder cls, MethodHolder method) {
		AnnotationHolder monitored = method.getAnnotations().get(Monitored.class.getName());
		if (monitored != null) {
			AnnotationValue value = monitored.getValue("value");
			String name = value != null ? (String) value.getString() : null;
			if (name != null && !name.isEmpty()) {
				return name;
			}
			AnnotationValue methodName = monitored.getValue("methodName");
			if (methodName != null) {
				Monitored.MethodNameRule mn = Monitored.MethodNameRule.valueOf(methodName.getEnumValue().getFieldName());
				if (mn == Monitored.MethodNameRule.SIMPLE) {
					return method.getName();
				} else if (mn == Monitored.MethodNameRule.FULL) {
					return null; // signal to use default (the full signature)
				}
				// else DEFAULT - fallthrough to class-level
			}
		}
		AnnotationHolder allMethods = cls.getAnnotations().get(Monitored.AllMethods.class.getName());

		if (allMethods == null) {
			return null; // signal to use default (the full signature)
		}

		AnnotationValue methodName = allMethods.getValue("methodName");
		if (methodName != null) {
			Monitored.MethodNameRule mn = Monitored.MethodNameRule.valueOf(methodName.getEnumValue().getFieldName());
			if (mn == Monitored.MethodNameRule.FULL) {
				return null;
			}
			// else FULL or DEFAULT - fallthrough to simple
		}

		return method.getName();
	}

	// ---------------------------------------------------------------------------------------------
	// Field + <clinit>
	// ---------------------------------------------------------------------------------------------

	private void ensureMonitorFieldAndClinit(ClassHolder cls, ClassHierarchy hierarchy, MonitorReferencePolicy.Policy policy) {
		if (cls.getField(MONITOR_FIELD) == null) {
			FieldHolder field = new FieldHolder(MONITOR_FIELD);
			field.setLevel(AccessLevel.PRIVATE);
			field.getModifiers().add(ElementModifier.STATIC);
			field.setType(ValueType.object(MONITOR_CLASS));
			cls.addField(field);
		}

		MethodHolder clinit =
			cls.getMethod(new MethodDescriptor("<clinit>", ValueType.VOID));
		if (clinit == null) {
			clinit = new MethodHolder(new MethodDescriptor("<clinit>", ValueType.VOID));
			clinit.setLevel(AccessLevel.PRIVATE);
			clinit.getModifiers().add(ElementModifier.STATIC);

			Program p = new Program();
			BasicBlock b = p.createBasicBlock();
			ExitInstruction ret = new ExitInstruction();
			b.add(ret);

			clinit.setProgram(p);
			cls.addMethod(clinit);
		}

		injectMonitorInit(cls, clinit, hierarchy, policy);
	}

	private void injectMonitorInit(ClassHolder cls, MethodHolder clinit, ClassHierarchy hierarchy, MonitorReferencePolicy.Policy policy) {
		Program program = clinit.getProgram();

		if (program == null) {
			program = new Program();
			clinit.setProgram(program);
			program.createBasicBlock().add(new ExitInstruction());
		}

		List<BasicBlock> existingBlocks = new ArrayList<>();

		for (int i = 0; i < program.basicBlockCount(); i++) {
			existingBlocks.add(program.basicBlockAt(i));
		}

		BasicBlock oldEntry = program.basicBlockCount() > 0
			? program.basicBlockAt(0)
			: program.createBasicBlock();


		program.pack();

		// make ourselves new entry block

		BasicBlock newEntry = program.createBasicBlock();

		existingBlocks.add(0, newEntry);

		program.rearrangeBasicBlocks(existingBlocks);

		program.createVariable(); // no-op to shift vars

		ProgramEmitter pe = ProgramEmitter.create(program, hierarchy);

		pe.enter(newEntry);

		MethodReference getRef = new MethodReference(MONITOR_CLASS,
			new MethodDescriptor("get",
				ValueType.object("java.lang.Object"),
				ValueType.object("java.lang.String"),
				ValueType.BOOLEAN,
				ValueType.object(MONITOR_CLASS)
			)
		);

		ValueEmitter monitor = pe.invoke(getRef,
			pe.constant(ValueType.object(cls.getName())),
			pe.constant(cls.getName().replace('/', '.')),
			pe.constant(policy == MonitorReferencePolicy.Policy.WEAK ? 1 : 0).cast(ValueType.BOOLEAN));

		pe.setField(cls.getName(), MONITOR_FIELD, monitor);

		pe.jump(oldEntry);
	}

	private MethodHolder copyNewName(String name, MethodHolder original) {
		MethodDescriptor originalDesc = original.getDescriptor();
		MethodDescriptor modifiedDesc = new MethodDescriptor(
			name,
			originalDesc.getSignature()
		);
		MethodHolder copy = new MethodHolder(modifiedDesc);
		copy.setLevel(original.getLevel());
		copy.getModifiers().addAll(original.getModifiers());
		if (original.hasProgram()) {
			copy.setProgram(ProgramUtils.copy(original.getProgram()));
		}
		ModelUtils.copyAnnotations(original.getAnnotations(), copy.getAnnotations());

		//TODO: ModelUtils has a setAnnotationsDefault here, but the copy method is private

		for (int i = 0; i < original.parameterCount(); i++) {
			ModelUtils.copyAnnotations(
				original.parameterAnnotation(i),
				copy.parameterAnnotation(i)
			);
		}
		return copy;
	}

	// ---------------------------------------------------------------------------------------------
	// Wrapping
	// ---------------------------------------------------------------------------------------------

	private void wrapWithMonitor(ClassHolder cls, MonitorMethod rules, ClassHierarchy hierarchy) {
		MethodHolder original = rules.original;
		String origName = original.getName();

		String implName = origName + METHOD_IMPL_SUFFIX;

		// 1) Create a copy with new name
		MethodHolder impl = copyNewName(implName, original);

		// 2) Clean up original
		original.getModifiers().remove(ElementModifier.NATIVE);
		original.getModifiers().remove(ElementModifier.ABSTRACT);

		// Drop @Monitored on wrapper
		original.getAnnotations().remove(Monitored.class.getName());

		Program program = new Program();
		original.setProgram(program);

		// 3) Add impl to class
		cls.addMethod(impl);

		generateWrapperBody(cls, rules, implName, hierarchy);
	}

	private void generateWrapperBody(ClassHolder cls,
									 MonitorMethod meta,
									 String implName, ClassHierarchy hierarchy) {
		MethodHolder wrapper = meta.original;
		Program p = wrapper.getProgram();
		MethodDescriptor desc = wrapper.getDescriptor();
		boolean isStatic = wrapper.getModifiers().contains(ElementModifier.STATIC);
		FieldReference monitorFieldRef = new FieldReference(cls.getName(), MONITOR_FIELD);

		// Blocks: entry -> tryBlock; catchBlock; (returns go directly from tryBlock)
		BasicBlock entry = p.createBasicBlock();
		BasicBlock tryBlock = p.createBasicBlock();
		BasicBlock catchBlock = p.createBasicBlock();

		// Try/catch wiring
		TryCatchBlock tcb = new TryCatchBlock();
		tcb.setExceptionType("java.lang.Throwable");
		tcb.setHandler(catchBlock);

		tryBlock.getTryCatchBlocks().add(tcb);

		tcb.setHandler(catchBlock);


		// We'll use ProgramEmitter per block for nicer code.
		ProgramEmitter pe = ProgramEmitter.create(p, hierarchy);


		// Signature string for monitor
		String sigString = meta.name != null ? meta.name : buildMethodSignatureString(cls, wrapper);

		// --- entry: resolve monitor, self, onEnter, jump to tryBlock ---

		pe.enter(entry);

		Variable selfVar = p.createVariable();

		// self = (isStatic ? null : this)
		ValueEmitter self = isStatic ? null :
			pe.var(selfVar, ValueType.object(cls.getName()));

		// mirror params
		ValueEmitter[] params = new ValueEmitter[desc.parameterCount()];
		for (int i = 0; i < params.length; i++) {
			params[i] = pe.newVar(desc.parameterType(i));
		}

		if (isStatic) {
			self = pe.constantNull(ValueType.object("java.lang.Object"));
		}

		// monitor = this.$awtea$monitor
		ValueEmitter monitor = pe.getField(
			monitorFieldRef,
			ValueType.object(MONITOR_CLASS)
		);

		ValueEmitter sigConst = pe.constant(sigString);

		// monitor.onEnter(self, sig)
		monitor.invoke(InvocationType.SPECIAL, onEnterRef, self, sigConst);

		// jump into try block
		pe.jump(tryBlock);

		// end of entry block, begin try block
		pe.enter(tryBlock);

		// --- tryBlock: call impl, onLeave, return result ---


		monitor = pe.getField(
			monitorFieldRef,
			ValueType.object(MONITOR_CLASS)
		);

		sigConst = pe.constant(sigString);

		MethodDescriptor implDesc = new MethodDescriptor(implName, desc.getSignature());
		MethodReference implRef = new MethodReference(cls.getName(), implDesc);

		ValueEmitter result = isStatic ? pe.invoke(implRef, params) : self.invoke(InvocationType.VIRTUAL, implRef, params);

		monitor.invoke(InvocationType.SPECIAL, onLeaveRef, self, sigConst);

		if (desc.getResultType() == ValueType.VOID) {
			pe.exit();
		} else {
			result.returnValue();
		}

		pe.enter(catchBlock);

		// --- catchBlock: onLeave + rethrow ---

		Variable var = p.createVariable();
		catchBlock.setExceptionVariable(var);

		monitor = pe.getField(
			monitorFieldRef,
			ValueType.object(MONITOR_CLASS)
		);

		sigConst = pe.constant(sigString);

		ValueEmitter exVar = pe.var(var, ValueType.object("java.lang.Throwable"));

		// monitor.onLeave(self, sig)

		monitor.invoke(InvocationType.SPECIAL, onThrownRef,
			self,
			sigConst,
			exVar
		);

		// rethrow exception
		exVar.raise();


	}

	// Very basic descriptor string, adjust if you want more precise JVM-style
	private String buildMethodSignatureString(ClassHolder cls, MethodHolder method) {
		MethodDescriptor desc = method.getDescriptor();
		StringBuilder sb = new StringBuilder();
		sb.append(method.getName()).append("(");
		for (int i = 0; i < desc.parameterCount(); i++) {
			sb.append(desc.parameterType(i).toString());
		}
		sb.append(")").append(desc.getResultType().toString());
		return sb.toString();
	}
}
