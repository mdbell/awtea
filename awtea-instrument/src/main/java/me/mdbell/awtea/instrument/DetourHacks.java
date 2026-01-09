package me.mdbell.awtea.instrument;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.model.*;
import org.teavm.model.instructions.InvokeInstruction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * ClassHolderTransformer that detours calls to selected methods/ctors
 * to your own static helper methods, using annotations for configuration.
 * <p>
 * Conventions:
 *
 * @DetourReceiver(target = Target.class)
 * public class SomeDetours {
 * <p>
 * // Instance method:
 * // Original: target.foo(a, b) : R
 * // Detour:   static R foo(Target self, A a, B b)
 * @DetourMethod("foo") public static ReturnType foo(Target self, A a, B b) { ... }
 * <p>
 * // Static method:
 * // Original: Target.bar(a, b) : R
 * // Detour:   static R bar(A a, B b)
 * @DetourMethod("bar") public static ReturnType bar(A a, B b) { ... }
 * <p>
 * // Constructor (factory-style):
 * // Original: new Target(a, b)
 * //           -> Target.<init>(A a, B b) : void
 * //
 * // Detour:   static Target create(A a, B b)
 * @DetourMethod("<init>") public static Target create(A a, B b) { ... }
 * }
 */
public class DetourHacks implements ClassHolderTransformer {

    /**
     * Internal representation of a single detour mapping.
     */
    private static class MethodDetour {
        /**
         * Descriptor of the original method to match (name + params + return).
         */
        final MethodDescriptor original;
        /**
         * Class that contains the detour implementation.
         */
        final Class<?> detourClass;
        /**
         * Name of the detour method inside detourClass.
         */
        final String detourName;

        MethodDetour(MethodDescriptor original, Class<?> detourClass, String detourName) {
            this.original = original;
            this.detourClass = detourClass;
            this.detourName = detourName;
        }
    }

    /**
     * key: original FQCN (e.g. java.lang.System)
     * val: detours for that class.
     */
    private final Map<String, MethodDetour[]> detours;

    private static final Logger log = LoggerFactory.getLogger(DetourHacks.class);

    /**
     * Construct a DetourHacks transformer for a given set of detour classes.
     * <p>
     * Each detour class must be annotated with @DetourReceiver, and contain
     * static methods annotated with @DetourMethod.
     */
    public DetourHacks(Collection<Class<?>> detourClasses) {
        this.detours = buildDetourMap(detourClasses);
    }

    /**
     * Convenience factory: pass your detour classes directly.
     * <p>
     * Example:
     * <p>
     * DetourHacks detour = DetourHacks.fromDetourClasses(
     * SystemDetour.class,
     * FieldDetour.class,
     * RandomAccessFileDetour.class,
     * ThreadDetour.class
     * );
     */
    public static DetourHacks fromDetourClasses(Class<?>... detourClasses) {
        return new DetourHacks(Arrays.asList(detourClasses));
    }

    /**
     * Convenience factory: load detour class names from a resource file.
     * <p>
     * The resource should be a UTF-8 text file on the classpath, with:
     * - one fully-qualified class name per line
     * - '#' starting a comment to end-of-line
     * - blank lines ignored
     * <p>
     * Example (META-INF/awtea.detours):
     * # core detours
     * me.mdbell.awtea.detour.SystemDetour
     * me.mdbell.awtea.detour.FieldDetour
     * me.mdbell.awtea.detour.RandomAccessFileDetour
     * me.mdbell.awtea.detour.ThreadDetour
     */
    public static DetourHacks fromResource(String resourcePath) {
        return fromResource(resourcePath, null);
    }

    /**
     * Convenience factory: load detour class names from a resource file.
     * <p>
     * The resource should be a UTF-8 text file on the classpath, with:
     * - one fully-qualified class name per line
     * - '#' starting a comment to end-of-line
     * - blank lines ignored
     * <p>
     * Example (META-INF/awtea.detours):
     * # core detours
     * me.mdbell.awtea.detour.SystemDetour
     * me.mdbell.awtea.detour.FieldDetour
     * me.mdbell.awtea.detour.RandomAccessFileDetour
     * me.mdbell.awtea.detour.ThreadDetour
     */
    public static DetourHacks fromResource(String resourcePath, ClassLoader cl) {
        if (cl == null) {
            cl = DetourHacks.class.getClassLoader();
        }

        List<Class<?>> classes = new ArrayList<>();

        try {
            Enumeration<URL> resources = cl.getResources(resourcePath);
            List<URL> urls = Collections.list(resources);
            for (URL resourceUrl : urls) {
                try (InputStream is = resourceUrl.openStream()) {
                    if (is == null) {
                        throw new IllegalArgumentException(
                                "Detour class list resource not found: " + resourcePath);
                    }
                    try (BufferedReader reader =
                                 new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
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
                                throw new RuntimeException(
                                        "Failed to load detour class '" + line +
                                                "' from resource " + resourcePath, e);
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(
                            "Failed to read detour class list from resource " + resourcePath, e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load detour class list resources: " + resourcePath, e);
        }
        return new DetourHacks(classes);
    }

    private static String stripComment(String line) {
        int idx = line.indexOf('#');
        return (idx >= 0) ? line.substring(0, idx) : line;
    }

    // ---------------------------------------------------------------------------------------------
    // Registry building
    // ---------------------------------------------------------------------------------------------

    private Map<String, MethodDetour[]> buildDetourMap(Collection<Class<?>> detourClasses) {
        Map<String, List<MethodDetour>> tmp = new HashMap<>();

        for (Class<?> detourClass : detourClasses) {
            DetourReceiver receiver = detourClass.getAnnotation(DetourReceiver.class);
            if (receiver == null) {
                log.warn("Detour class missing @DetourReceiver: {} - skipping", detourClass.getName());
                continue;
            }

            Class<?> targetType = receiver.target();
            String targetClassName = targetType.getName();

            for (Method m : detourClass.getDeclaredMethods()) {
                DetourMethod dm = m.getAnnotation(DetourMethod.class);
                if (dm == null) {
                    continue;
                }

                if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                    log.warn("Detour method not static: {} - skipping", m);
                    continue;
                }
                if (m.getAnnotation(NoDetours.class) != null) {
                    // explicit opt-out
                    continue;
                }

                String originalName = dm.value().isEmpty()
                        ? m.getName()
                        : dm.value();

                if (dm.constructor()) {
                    // convenince flag for constructor detours, so we don't have to use the internal name
                    originalName = "<init>";
                }

                MethodDescriptor originalDesc =
                        buildOriginalDescriptorFromDetour(targetType, originalName, m);

                MethodDetour md = new MethodDetour(
                        originalDesc,
                        detourClass,
                        m.getName()
                );

                tmp.computeIfAbsent(targetClassName, k -> new ArrayList<>())
                        .add(md);
            }
        }

        Map<String, MethodDetour[]> result = new HashMap<>();
        tmp.forEach((k, list) -> result.put(k, list.toArray(new MethodDetour[0])));

        // print out detour map for debugging
        log.debug("Detour map:");
        result.forEach((targetClass, detourList) -> {
            for (MethodDetour detour : detourList) {
                log.debug("  {}.{} -> {}.{}{}",
                        targetClass, detour.original.getName(),
                        detour.detourClass.getName(), detour.detourName,
                        detour.original.signatureToString());
            }
        });

        return result;
    }

    /**
     * Given a detour method and the target class + original name, produce the descriptor
     * of the original method that this detour is meant to replace.
     * <p>
     * Conventions:
     * <p>
     * 1) Constructor detour (factory-style):
     *
     * @DetourMethod("<init>") public static Target factory(P1 p1, P2 p2, ...) { ... }
     * <p>
     * Original: Target.<init>(P1 p1, P2 p2, ...) : void
     * <p>
     * 2) Instance method detour:
     * @DetourMethod("name") public static R name(Target self, P1 p1, P2 p2, ...) { ... }
     * <p>
     * Original: Target.name(P1 p1, P2 p2, ...) : R
     * <p>
     * 3) Static method detour:
     * @DetourMethod("name") public static R name(P1 p1, P2 p2, ...) { ... }
     * <p>
     * Original: Target.name(P1 p1, P2 p2, ...) : R
     */
    private MethodDescriptor buildOriginalDescriptorFromDetour(Class<?> targetType,
                                                               String originalName,
                                                               Method detourMethod) {
        Class<?>[] detourParams = detourMethod.getParameterTypes();
        Class<?> detourReturn = detourMethod.getReturnType();

        if ("<init>".equals(originalName)) {
            // Constructor detour: static Target factory(p1, p2, ...)
            if (detourReturn != targetType) {
                throw new IllegalArgumentException(
                        "Constructor detour must return " + targetType.getName() + ": " + detourMethod);
            }

            // Original ctor signature: (p1, p2, ...) -> void
            Class<?>[] signature = Arrays.copyOf(detourParams, detourParams.length + 1);
            signature[signature.length - 1] = void.class;

            return new MethodDescriptor(originalName, signature);
        }

        // Instance method detour: static R name(Target self, p1, p2, ...)
        if (detourParams.length > 0 && detourParams[0] == targetType) {
            Class<?>[] origParams = Arrays.copyOfRange(detourParams, 1, detourParams.length);
            Class<?>[] signature = Arrays.copyOf(origParams, origParams.length + 1);
            signature[signature.length - 1] = detourReturn;
            return new MethodDescriptor(originalName, signature);
        }

        // Static method detour: static R name(p1, p2, ...)
        Class<?>[] signature = Arrays.copyOf(detourParams, detourParams.length + 1);
        signature[signature.length - 1] = detourReturn;
        return new MethodDescriptor(originalName, signature);
    }

    // ---------------------------------------------------------------------------------------------
    // Transformer implementation
    // ---------------------------------------------------------------------------------------------

    @Override
    public void transformClass(ClassHolder classHolder, ClassHolderTransformerContext context) {
        // Allow opting out at the class level with @NoDetours
        if (classHolder.getAnnotations().get(NoDetours.class.getName()) != null) {
            return;
        }

        for (MethodHolder method : classHolder.getMethods()) {
            transformMethod(method);
        }
    }

    private void transformMethod(MethodHolder method) {
        if (!method.hasProgram()) {
            return;
        }

        Program program = method.getProgram();
        for (BasicBlock block : program.getBasicBlocks()) {
            transformBlock(method, block);
        }
    }

    private void transformBlock(MethodHolder owner, BasicBlock block) {
        for (Instruction insn : block) {
            if (insn instanceof InvokeInstruction) {
                transformInvoke(owner, (InvokeInstruction) insn);
            }
        }
    }

    private void transformInvoke(MethodHolder owner, InvokeInstruction invoke) {
        MethodReference ref = invoke.getMethod();
        MethodDetour[] detoursForClass = detours.get(ref.getClassName());
        if (detoursForClass == null) {
            return;
        }

        for (MethodDetour detour : detoursForClass) {
            if (!ref.getDescriptor().equals(detour.original)) {
                continue;
            }

            String originalName = ref.getName();
            ValueType[] sig = detour.original.getSignature(); // [p1, ..., pN, returnType]
            Variable thisVar = invoke.getInstance();

            if ("<init>".equals(originalName)) {
                // Skip detouring calls to our own ctor (this()/super()).
                if (owner.getOwnerName().equals(ref.getClassName())
                        && thisVar != null && thisVar.getIndex() == 0) {
                    continue;
                }

                // Constructor detour:
                // Original sig: [p1, p2, ..., void]
                // Detour sig:   [p1, p2, ..., Target]
                //
                // Original had no receiver (void); we now:
                //   thisVar = DetourClass.factory(p1, p2, ...);

                invoke.setReceiver(thisVar);
                invoke.setInstance(null); // static call

                ValueType[] newSig = sig.clone();
                // Replace return type (last element) with Target type
                newSig[newSig.length - 1] = ValueType.object(ref.getClassName());

                MethodDescriptor detourDesc = new MethodDescriptor(detour.detourName, newSig);
                invoke.setMethod(new MethodReference(detour.detourClass.getName(), detourDesc));
                continue;
            }

            if (thisVar != null) {
                // Instance method detour:
                // Original sig: [p1, p2, ..., R]
                // Detour sig:   [Target, p1, p2, ..., R]
                //
                //   this.method(p1, p2, ...)
                // -> DetourClass.method(this, p1, p2, ...)

                List<Variable> newArgs = new ArrayList<>();
                newArgs.add(thisVar);
                newArgs.addAll(invoke.getArguments());
                invoke.setArguments(newArgs.toArray(new Variable[0]));
                invoke.setInstance(null); // static call

                ValueType[] newSig = new ValueType[sig.length + 1];
                // First param = Target
                newSig[0] = ValueType.object(ref.getClassName());
                // Copy original params (excluding return type) right after
                System.arraycopy(sig, 0, newSig, 1, sig.length - 1);
                // Last element = original return type
                newSig[newSig.length - 1] = sig[sig.length - 1];

                MethodDescriptor detourDesc = new MethodDescriptor(detour.detourName, newSig);
                invoke.setMethod(new MethodReference(detour.detourClass.getName(), detourDesc));
            } else {
                // Static method detour:
                // Signature identical; just swap owner/name.
                MethodDescriptor detourDesc = new MethodDescriptor(detour.detourName, sig);
                invoke.setMethod(new MethodReference(detour.detourClass.getName(), detourDesc));
            }
        }
    }
}
