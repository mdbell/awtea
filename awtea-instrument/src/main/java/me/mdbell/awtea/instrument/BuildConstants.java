package me.mdbell.awtea.instrument;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyListener;
import org.teavm.dependency.FieldDependency;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.StringConstantInstruction;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Rewrites {@link BuildConstant} placeholder constants to values supplied by
 * the build. No class registration is needed: every compiled class is scanned
 * for the annotation, and keys are matched against the values set here.
 * <p>
 * Edits the javac-produced constant in place instead of synthesizing a
 * replacement program — hand-built IR has to honor renderer invariants the
 * model API doesn't enforce. The strict body convention (exactly
 * {@code return "<placeholder>";}) is what makes the in-place edit safe, so
 * violations fail the build, as does a reachable probe whose key has no
 * supplied value.
 * <p>
 * Register a single instance per build:
 * <pre>
 * BuildConstants constants = new BuildConstants()
 *         .set("compile.time", timestamp);
 * host.add(constants);
 * host.add(constants.unusedValueVerifier());
 * </pre>
 */
public class BuildConstants implements ClassHolderTransformer {

    private static final Logger log = LoggerFactory.getLogger(BuildConstants.class);

    private final Map<String, String> values = new LinkedHashMap<>();
    private final Map<String, Boolean> flags = new LinkedHashMap<>();
    private final Set<String> consumedKeys = new HashSet<>();

    /** Supplies the value a {@link BuildConstant} probe with this key receives. */
    public BuildConstants set(String key, String value) {
        values.put(key, value);
        return this;
    }

    /**
     * Supplies the value a {@link BuildFlag} probe with this key receives.
     * Supply the key in every build — {@code setFlag("dev", isDev)} — rather
     * than only when true, so a typo'd or forgotten key stays a build error
     * instead of a silently-false flag.
     */
    public BuildConstants setFlag(String key, boolean value) {
        flags.put(key, value);
        return this;
    }

    @Override
    public void transformClass(ClassHolder classHolder, ClassHolderTransformerContext context) {
        for (MethodHolder method : classHolder.getMethods()) {
            AnnotationHolder flagAnnotation =
                    method.getAnnotations().get(BuildFlag.class.getName());
            AnnotationHolder annotation =
                    method.getAnnotations().get(BuildConstant.class.getName());
            if (flagAnnotation != null && annotation != null) {
                throw new IllegalStateException(
                        "Method carries both @BuildConstant and @BuildFlag: "
                                + classHolder.getName() + "." + method.getName());
            }
            if (flagAnnotation != null) {
                rewriteFlag(classHolder, method, flagAnnotation);
                continue;
            }
            if (annotation == null) {
                continue;
            }
            String location = classHolder.getName() + "." + method.getName();
            String key = annotation.getValue("value").getString();
            String value = values.get(key);
            if (value == null) {
                throw new IllegalStateException(
                        "@BuildConstant key '" + key + "' at " + location
                                + " has no supplied value. Either the probe and build plugin are"
                                + " out of sync, or a second BuildConstants instance is registered"
                                + " - every instance scans every probe, so all keys must be"
                                + " supplied through a single instance per build");
            }

            if (!method.hasModifier(ElementModifier.STATIC)
                    || !ValueType.parse(String.class).equals(method.getResultType())
                    || !method.hasProgram()) {
                throw new IllegalStateException(
                        "@BuildConstant method must be a static String method with a body: " + location);
            }

            int replaced = 0;
            for (BasicBlock block : method.getProgram().getBasicBlocks()) {
                for (Instruction insn : block) {
                    if (insn instanceof StringConstantInstruction) {
                        ((StringConstantInstruction) insn).setConstant(value);
                        replaced++;
                    }
                }
            }
            if (replaced != 1) {
                throw new IllegalStateException(
                        "@BuildConstant method body must be exactly 'return \"<placeholder>\";'"
                                + " (found " + replaced + " string constants): " + location);
            }

            consumedKeys.add(key);
            log.debug("Rewrote @BuildConstant '{}' at {}", key, location);
        }
    }

    /**
     * Rewrites a {@link BuildFlag} probe. The body must be exactly
     * {@code return false;}; a supplied value of true flips the constant, a
     * supplied false leaves the source default standing (but still marks the
     * key consumed).
     */
    private void rewriteFlag(ClassHolder classHolder, MethodHolder method,
                             AnnotationHolder annotation) {
        String location = classHolder.getName() + "." + method.getName();
        String key = annotation.getValue("value").getString();
        Boolean value = flags.get(key);
        if (value == null) {
            throw new IllegalStateException(
                    "@BuildFlag key '" + key + "' at " + location
                            + " has no supplied value. Supply it in every build"
                            + " (setFlag(\"" + key + "\", ...)) - and through a single"
                            + " BuildConstants instance: every registered instance scans every"
                            + " probe, so a second instance fails on keys it doesn't own");
        }

        if (!method.hasModifier(ElementModifier.STATIC)
                || !ValueType.parse(boolean.class).equals(method.getResultType())
                || !method.hasProgram()) {
            throw new IllegalStateException(
                    "@BuildFlag method must be a static boolean method with a body: " + location);
        }

        int constants = 0;
        for (BasicBlock block : method.getProgram().getBasicBlocks()) {
            for (Instruction insn : block) {
                if (insn instanceof IntegerConstantInstruction) {
                    IntegerConstantInstruction constant = (IntegerConstantInstruction) insn;
                    if (constant.getConstant() != 0) {
                        throw new IllegalStateException(
                                "@BuildFlag method body must be exactly 'return false;'"
                                        + " (the source default is off): " + location);
                    }
                    if (value) {
                        constant.setConstant(1);
                    }
                    constants++;
                }
            }
        }
        if (constants != 1) {
            throw new IllegalStateException(
                    "@BuildFlag method body must be exactly 'return false;'"
                            + " (found " + constants + " integer constants): " + location);
        }

        consumedKeys.add(key);
        log.debug("Rewrote @BuildFlag '{}' at {} to {}", key, location, value);
    }

    /**
     * Optional companion listener: once dependency analysis completes,
     * reports (as warnings) every supplied value whose probe was never
     * reached — a typo'd key, or a probe class that fell out of the build.
     * Warnings rather than errors: an unreachable probe is dead code, not a
     * wrong constant.
     */
    public DependencyListener unusedValueVerifier() {
        return new DependencyListener() {
            @Override
            public void started(DependencyAgent agent) {
            }

            @Override
            public void classReached(DependencyAgent agent, String className) {
            }

            @Override
            public void methodReached(DependencyAgent agent, MethodDependency method) {
            }

            @Override
            public void fieldReached(DependencyAgent agent, FieldDependency field) {
            }

            @Override
            public void completing(DependencyAgent agent) {
                for (String key : values.keySet()) {
                    if (!consumedKeys.contains(key)) {
                        agent.getDiagnostics().warning(null,
                                "Supplied @BuildConstant value was never consumed: '" + key
                                        + "' (typo'd key, or the probe is unreachable)");
                    }
                }
                for (String key : flags.keySet()) {
                    if (!consumedKeys.contains(key)) {
                        agent.getDiagnostics().warning(null,
                                "Supplied @BuildFlag value was never consumed: '" + key
                                        + "' (typo'd key, or the probe is unreachable)");
                    }
                }
            }

            @Override
            public void complete() {
            }
        };
    }
}
