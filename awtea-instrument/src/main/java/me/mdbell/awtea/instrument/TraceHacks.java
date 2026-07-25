package me.mdbell.awtea.instrument;

import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.StringConstantInstruction;

/**
 * Debug transformer: injects {@link TraceLog#enter(String)} at the entry of
 * every method whose declaring class matches one of the configured name
 * prefixes. Enabled by the TeaVM property {@code awtea.trace} — a
 * comma-separated list of class-name prefixes (e.g. {@code jagex.}). Off (and
 * zero-cost) when the property is absent.
 *
 * The injected callee is non-suspending by design (see TraceLog), so tracing
 * does not change any method's suspendability classification.
 */
public final class TraceHacks implements ClassHolderTransformer {

    private static final MethodReference ENTER = new MethodReference(
            TraceLog.class.getName(), "enter",
            ValueType.object("java.lang.String"), ValueType.VOID);

    private final String[] prefixes;

    public TraceHacks(String prefixList) {
        this.prefixes = prefixList.split(",");
        for (int i = 0; i < prefixes.length; i++) {
            prefixes[i] = prefixes[i].trim();
        }
    }

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        String name = cls.getName();
        if (name.startsWith(TraceLog.class.getName())) {
            return;
        }
        boolean match = false;
        for (String p : prefixes) {
            if (!p.isEmpty() && name.startsWith(p)) {
                match = true;
                break;
            }
        }
        if (!match) {
            return;
        }
        for (MethodHolder method : cls.getMethods()) {
            Program program = method.getProgram();
            if (program == null || program.basicBlockCount() == 0) {
                continue;
            }
            BasicBlock entry = program.basicBlockAt(0);
            Variable sigVar = program.createVariable();
            StringConstantInstruction sc = new StringConstantInstruction();
            sc.setReceiver(sigVar);
            sc.setConstant(shortName(name) + "." + method.getName());
            InvokeInstruction inv = new InvokeInstruction();
            inv.setType(InvocationType.SPECIAL);
            inv.setMethod(ENTER);
            inv.setArguments(sigVar);
            entry.addFirst(inv);
            entry.addFirst(sc);
        }
    }

    private static String shortName(String className) {
        int i = className.lastIndexOf('.');
        return i >= 0 ? className.substring(i + 1) : className;
    }
}
