package me.mdbell.awtea.transform;

import org.teavm.model.*;
import org.teavm.model.instructions.InvokeInstruction;
import me.mdbell.awtea.detour.NoDetours;
import me.mdbell.awtea.detour.SystemDetour;

import java.util.HashMap;
import java.util.Map;

public class DetourHacks implements ClassHolderTransformer {

    static class MethodDetour {
        public final MethodDescriptor desc;
        public final Class<?> newClass;

        public MethodDetour(MethodDescriptor descriptor, Class<?> newClass) {
            this.desc = descriptor;
            this.newClass = newClass;
        }
    }

    Map<String, MethodDetour[]> detours = new HashMap<>() {{
        put(System.class.getName(), new MethodDetour[]{
                new MethodDetour(MethodDescriptor.parse("exit(I)V"), SystemDetour.class)
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

        method.getProgram().getBasicBlocks().forEach(this::transformBlock);
    }

    private void transformBlock(BasicBlock instructions) {
        instructions.forEach(instruction -> {
            if (instruction instanceof InvokeInstruction) {
                transformInvoke((InvokeInstruction) instruction);
            }
        });
    }

    private void transformInvoke(InvokeInstruction invoke) {
        MethodReference ref = invoke.getMethod();
        MethodDetour[] detours = this.detours.get(ref.getClassName());

        if (detours == null) {
            return;
        }

        for (MethodDetour detour : detours) {
            MethodDescriptor desc = detour.desc;
            if (ref.getDescriptor().equals(desc)) {
                invoke.setMethod(new MethodReference(detour.newClass.getName(), detour.desc));
            }
        }
    }
}
