package me.mdbell.awtea.instrument;

import me.mdbell.awtea.Helper;
import org.teavm.model.*;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;

/**
 * This transformer replaces the creation of 3D arrays with a call to a helper method.
 * This is required because TeaVM has some sort of bug when creating 3D arrays, resulting in
 * a null dereference.
 */
public class ArrayHacks implements ClassHolderTransformer {
	@Override
	public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
		for (MethodHolder method : cls.getMethods()) {
			replace3dArrayCreation(method);
		}
	}

	private void replace3dArrayCreation(MethodHolder method) {
		if (!method.hasProgram()) {
			return;
		}
		// we are going to replace the new byte[][][] with a call to our method in Helper
		Program p = method.getProgram();
		for (BasicBlock block : p.getBasicBlocks()) {
			for (Instruction insn : block) {
				if (insn instanceof ConstructMultiArrayInstruction) {
					ConstructMultiArrayInstruction multi = (ConstructMultiArrayInstruction) insn;
					String methodName = getNewMethodName(multi.getItemType().toString());
					if (methodName == null) {
						continue;
					}

					// we need to reconstruct the method descriptor
					ValueType[] types = new ValueType[multi.getDimensions().size() + 1];
					for (int i = 0; i < multi.getDimensions().size(); i++) {
						types[i] = ValueType.INTEGER;
					}
					types[types.length - 1] = multi.getItemType();

					Variable[] args = new Variable[multi.getDimensions().size()];
					for (int i = 0; i < multi.getDimensions().size(); i++) {
						args[i] = multi.getDimensions().get(i);
					}

					InvokeInstruction invoke = new InvokeInstruction();
					invoke.setType(InvocationType.SPECIAL);
					invoke.setMethod(new MethodReference(Helper.class.getName(), new MethodDescriptor(
						methodName, types)
					));
					invoke.setReceiver(multi.getReceiver());
					invoke.setArguments(args);
					multi.replace(invoke);
				}
			}
		}
	}

	private String getNewMethodName(String type) {
		switch (type) {
			case "[[[B":
				return "create3DByteArray";
			case "[[[I":
				return "create3DIntArray";
			default:
				return null;
		}
	}
}
