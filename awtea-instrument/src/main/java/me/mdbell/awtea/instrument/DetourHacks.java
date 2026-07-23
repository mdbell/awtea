package me.mdbell.awtea.instrument;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyListener;
import org.teavm.dependency.FieldDependency;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.*;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;
import org.teavm.model.util.BasicBlockSplitter;
import org.teavm.model.util.ProgramUtils;

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
 * <p>
 * // Advice (call inserted around the original instead of replacing it):
 * // see {@link Before} and {@link After} for the signature conventions.
 * @Before("foo") public static void beforeFoo(Target self, A a, B b) { ... }
 * @After("foo") public static void afterFoo(Target self, A a, B b) { ... }
 * <p>
 * // Guard (decides per call whether the original runs; see {@link Guard}):
 * @Guard("foo") public static void guardFoo(Target self, A a, B b, Interception ci) { ... }
 * <p>
 * // Filter (pipes the original's result; see {@link Filter}):
 * @Filter("foo") public static R filterFoo(Target self, A a, B b, R result) { ... }
 * }
 */
public class DetourHacks implements ClassHolderTransformer {

    private enum Kind {
        /** Replace the call with the detour (the original @DetourMethod). */
        REPLACE,
        /** Insert an advice call before the original call. */
        BEFORE,
        /** Insert an advice call after the original call. */
        AFTER,
        /** Advice on both normal and exceptional exit (see {@link Finally}). */
        FINALLY,
        /** Let a @Guard decide whether the original runs (see {@link Guard}). */
        GUARD,
        /** Pipe the original's result through a @Filter (see {@link Filter}). */
        FILTER,
        /** Pipe every read of a field through a hook (see {@link FieldGet}). */
        FIELD_GET,
        /** Pipe every write of a field through a hook (see {@link FieldSet}). */
        FIELD_SET,
        /** Pipe element reads of a field's array through a hook (see {@link ElementGet}). */
        ELEMENT_GET,
        /** Pipe element writes of a field's array through a hook (see {@link ElementSet}). */
        ELEMENT_SET
    }

    /** Where an array variable came from: a read of a specific field. */
    private static class FieldOrigin {
        final FieldReference field;
        final Variable instance;
        final ValueType fieldType;

        FieldOrigin(FieldReference field, Variable instance, ValueType fieldType) {
            this.field = field;
            this.instance = instance;
            this.fieldType = fieldType;
        }
    }

    /**
     * Internal representation of a single detour mapping.
     */
    private static class MethodDetour {
        final Kind kind;
        /**
         * REPLACE/FILTER: descriptor of the original method to match
         * (name + params + return). Null for the other kinds, which match
         * without the return type.
         */
        final MethodDescriptor original;
        /**
         * Advice: original method name and parameter types to match. The
         * original's return type is unknown to (and irrelevant for) advice,
         * so it does not participate in matching. Null for REPLACE.
         * <p>
         * FIELD_GET/FIELD_SET reuse these: the name is the field name and
         * the params hold a single element, the field's type.
         */
        final String adviceTargetName;
        final ValueType[] adviceTargetParams;
        /**
         * Advice: whether the advice method's first parameter is the receiver
         * (instance-method form) as opposed to the static-method form.
         */
        final boolean adviceHasSelf;
        /**
         * Advice: descriptor of the static advice method itself, used to
         * build the inserted call. Null for REPLACE.
         */
        final MethodDescriptor adviceDescriptor;
        /**
         * Class that contains the detour implementation.
         */
        final Class<?> detourClass;
        /**
         * Name of the detour method inside detourClass.
         */
        final String detourName;
        /**
         * Call sites this detour actually rewrote/instrumented. Zero at the
         * end of dependency analysis means the original was renamed, its
         * signature drifted, or the detour is dead — see
         * {@link #zeroMatchVerifier(boolean)}.
         */
        int matchedSites;
        /**
         * FQCNs of the classes whose sites this detour may bind, from
         * {@link Callers}; null means unrestricted.
         */
        Set<String> callers;

        MethodDetour(MethodDescriptor original, Class<?> detourClass, String detourName) {
            this.kind = Kind.REPLACE;
            this.original = original;
            this.adviceTargetName = null;
            this.adviceTargetParams = null;
            this.adviceHasSelf = false;
            this.adviceDescriptor = null;
            this.detourClass = detourClass;
            this.detourName = detourName;
        }

        MethodDetour(Kind kind, String adviceTargetName, ValueType[] adviceTargetParams,
                     boolean adviceHasSelf, MethodDescriptor adviceDescriptor,
                     Class<?> detourClass, String detourName) {
            this.kind = kind;
            this.original = null;
            this.adviceTargetName = adviceTargetName;
            this.adviceTargetParams = adviceTargetParams;
            this.adviceHasSelf = adviceHasSelf;
            this.adviceDescriptor = adviceDescriptor;
            this.detourClass = detourClass;
            this.detourName = detourName;
        }

        /** FILTER: exact original descriptor plus the filter's own call shape. */
        MethodDetour(MethodDescriptor original, boolean adviceHasSelf,
                     MethodDescriptor adviceDescriptor, Class<?> detourClass, String detourName) {
            this.kind = Kind.FILTER;
            this.original = original;
            this.adviceTargetName = null;
            this.adviceTargetParams = null;
            this.adviceHasSelf = adviceHasSelf;
            this.adviceDescriptor = adviceDescriptor;
            this.detourClass = detourClass;
            this.detourName = detourName;
        }
    }

    /**
     * key: original FQCN (e.g. java.lang.System)
     * val: detours for that class.
     */
    private final Map<String, MethodDetour[]> detours;

    /**
     * FQCNs of the registered detour classes themselves (the ones carrying
     * a valid @DetourReceiver). Their @DetourApplied probe methods get
     * rewritten to report that this build applied them.
     */
    private final Set<String> registeredDetourClasses;

    /**
     * Whether any element hooks are registered at all — methods skip the
     * per-method array-origin scan entirely when none are.
     */
    private boolean hasElementHooks;

    private static final Logger log = LoggerFactory.getLogger(DetourHacks.class);

    /**
     * Construct a DetourHacks transformer for a given set of detour classes.
     * <p>
     * Each detour class must be annotated with @DetourReceiver, and contain
     * static methods annotated with @DetourMethod.
     */
    public DetourHacks(Collection<Class<?>> detourClasses) {
        this.registeredDetourClasses = new HashSet<>();
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
            if (detourClass.getAnnotation(DisableDetour.class) != null) {
                log.info("Detour class disabled via @DisableDetour: {}", detourClass.getName());
                continue;
            }
            DetourReceiver receiver = detourClass.getAnnotation(DetourReceiver.class);
            if (receiver == null) {
                throw new IllegalArgumentException(
                        "Detour class missing @DetourReceiver: " + detourClass.getName()
                                + " (annotate with @DisableDetour if intentionally disabled)");
            }

            Class<?> targetType = receiver.target();
            String targetClassName = targetType.getName();
            registeredDetourClasses.add(detourClass.getName());

            for (Method m : detourClass.getDeclaredMethods()) {
                if (m.getAnnotation(NoDetours.class) != null) {
                    // explicit opt-out
                    continue;
                }
                if (m.getAnnotation(DisableDetour.class) != null) {
                    log.info("Detour method disabled via @DisableDetour: {}", m);
                    continue;
                }

                Before before = m.getAnnotation(Before.class);
                After after = m.getAnnotation(After.class);
                Finally finallyAdvice = m.getAnnotation(Finally.class);
                Guard guard = m.getAnnotation(Guard.class);
                Filter filter = m.getAnnotation(Filter.class);
                FieldGet fieldGet = m.getAnnotation(FieldGet.class);
                FieldSet fieldSet = m.getAnnotation(FieldSet.class);
                ElementGet elementGet = m.getAnnotation(ElementGet.class);
                ElementSet elementSet = m.getAnnotation(ElementSet.class);
                DetourMethod dm = m.getAnnotation(DetourMethod.class);

                Callers callersAnnotation = m.getAnnotation(Callers.class);

                int annotationCount = (before != null ? 1 : 0) + (after != null ? 1 : 0)
                        + (finallyAdvice != null ? 1 : 0)
                        + (guard != null ? 1 : 0) + (filter != null ? 1 : 0)
                        + (fieldGet != null ? 1 : 0) + (fieldSet != null ? 1 : 0)
                        + (elementGet != null ? 1 : 0) + (elementSet != null ? 1 : 0)
                        + (dm != null ? 1 : 0);
                if (annotationCount == 0) {
                    if (callersAnnotation != null) {
                        throw new IllegalArgumentException(
                                "@Callers without a detour annotation to filter: " + m);
                    }
                    continue;
                }
                if (annotationCount > 1) {
                    throw new IllegalArgumentException(
                            "Method carries more than one detour annotation: " + m);
                }
                Set<String> callers = null;
                if (callersAnnotation != null) {
                    if (callersAnnotation.value().length == 0) {
                        throw new IllegalArgumentException(
                                "@Callers must list at least one class (use @DisableDetour to turn"
                                        + " a detour off): " + m);
                    }
                    callers = new HashSet<>();
                    for (Class<?> caller : callersAnnotation.value()) {
                        callers.add(caller.getName());
                    }
                }

                if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                    if (dm != null) {
                        log.warn("Detour method not static: {} - skipping", m);
                        continue;
                    }
                    // advice/guard/filter are new features; be strict rather than lenient
                    throw new IllegalArgumentException("Advice method must be static: " + m);
                }

                if (dm == null) {
                    MethodDetour built;
                    if (fieldGet != null) {
                        built = buildFieldHook(Kind.FIELD_GET, targetType, fieldGet.value(), m);
                    } else if (fieldSet != null) {
                        built = buildFieldHook(Kind.FIELD_SET, targetType, fieldSet.value(), m);
                    } else if (elementGet != null) {
                        built = buildElementHook(Kind.ELEMENT_GET, targetType, elementGet.value(), m);
                        hasElementHooks = true;
                    } else if (elementSet != null) {
                        built = buildElementHook(Kind.ELEMENT_SET, targetType, elementSet.value(), m);
                        hasElementHooks = true;
                    } else {
                        Kind kind = before != null ? Kind.BEFORE
                                : after != null ? Kind.AFTER
                                : finallyAdvice != null ? Kind.FINALLY
                                : guard != null ? Kind.GUARD
                                : Kind.FILTER;
                        String name = before != null ? before.value()
                                : after != null ? after.value()
                                : finallyAdvice != null ? finallyAdvice.value()
                                : guard != null ? guard.value()
                                : filter.value();
                        String originalName = name.isEmpty() ? m.getName() : name;
                        switch (kind) {
                            case BEFORE:
                            case AFTER:
                            case FINALLY:
                                built = buildAdvice(kind, targetType, originalName, m);
                                break;
                            case GUARD:
                                built = buildGuard(targetType, originalName, m);
                                break;
                            default:
                                built = buildFilter(targetType, originalName, m);
                                break;
                        }
                    }
                    built.callers = callers;
                    tmp.computeIfAbsent(targetClassName, k -> new ArrayList<>()).add(built);
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
                md.callers = callers;

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
                if (detour.kind == Kind.REPLACE) {
                    log.debug("  {}.{} -> {}.{}{}",
                            targetClass, detour.original.getName(),
                            detour.detourClass.getName(), detour.detourName,
                            detour.original.signatureToString());
                } else {
                    String originalName = detour.original != null
                            ? detour.original.getName()
                            : detour.adviceTargetName;
                    log.debug("  {} {}.{} -> {}.{}{}",
                            detour.kind, targetClass, originalName,
                            detour.detourClass.getName(), detour.detourName,
                            detour.adviceDescriptor.signatureToString());
                }
            }
        });

        return result;
    }

    /**
     * Builds an advice mapping from a @Before/@After method. Mirrors the
     * signature conventions of replacement detours (leading Target parameter
     * selects the instance-method form) minus the return value: advice never
     * replaces the call, so it must be void and the original's return type
     * plays no part in matching.
     */
    private MethodDetour buildAdvice(Kind kind, Class<?> targetType, String originalName,
                                     Method adviceMethod) {
        if ("<init>".equals(originalName)) {
            throw new IllegalArgumentException(
                    "Advice on constructors is not supported: " + adviceMethod);
        }
        if (adviceMethod.getReturnType() != void.class) {
            throw new IllegalArgumentException("Advice method must return void: " + adviceMethod);
        }

        Class<?>[] adviceParams = adviceMethod.getParameterTypes();
        boolean hasSelf = adviceParams.length > 0 && adviceParams[0] == targetType;
        Class<?>[] originalParams = hasSelf
                ? Arrays.copyOfRange(adviceParams, 1, adviceParams.length)
                : adviceParams;

        ValueType[] targetParamTypes = new ValueType[originalParams.length];
        for (int i = 0; i < originalParams.length; i++) {
            targetParamTypes[i] = ValueType.parse(originalParams[i]);
        }

        Class<?>[] signature = Arrays.copyOf(adviceParams, adviceParams.length + 1);
        signature[signature.length - 1] = void.class;
        MethodDescriptor adviceDesc = new MethodDescriptor(adviceMethod.getName(), signature);

        return new MethodDetour(kind, originalName, targetParamTypes, hasSelf, adviceDesc,
                adviceMethod.getDeclaringClass(), adviceMethod.getName());
    }

    /**
     * Builds a guard mapping from a @Guard method: like advice, but with a
     * mandatory trailing {@link Interception} parameter that is excluded from
     * matching. The original's return type is unknown here; the call-site
     * transform recovers it from each matched invoke.
     */
    private MethodDetour buildGuard(Class<?> targetType, String originalName, Method guardMethod) {
        if ("<init>".equals(originalName)) {
            throw new IllegalArgumentException(
                    "Guards on constructors are not supported: " + guardMethod);
        }
        if (guardMethod.getReturnType() != void.class) {
            throw new IllegalArgumentException("Guard method must return void: " + guardMethod);
        }
        Class<?>[] guardParams = guardMethod.getParameterTypes();
        if (guardParams.length == 0 || guardParams[guardParams.length - 1] != Interception.class) {
            throw new IllegalArgumentException(
                    "Guard method must take a trailing Interception parameter: " + guardMethod);
        }

        boolean hasSelf = guardParams.length > 1 && guardParams[0] == targetType;
        Class<?>[] originalParams = Arrays.copyOfRange(
                guardParams, hasSelf ? 1 : 0, guardParams.length - 1);

        ValueType[] targetParamTypes = new ValueType[originalParams.length];
        for (int i = 0; i < originalParams.length; i++) {
            targetParamTypes[i] = ValueType.parse(originalParams[i]);
        }

        Class<?>[] signature = Arrays.copyOf(guardParams, guardParams.length + 1);
        signature[signature.length - 1] = void.class;
        MethodDescriptor guardDesc = new MethodDescriptor(guardMethod.getName(), signature);

        return new MethodDetour(Kind.GUARD, originalName, targetParamTypes, hasSelf, guardDesc,
                guardMethod.getDeclaringClass(), guardMethod.getName());
    }

    /**
     * Builds a filter mapping from a @Filter method: the trailing parameter
     * is the original's return value and doubles as its declared return type,
     * which restores exact-descriptor matching for filters.
     */
    private MethodDetour buildFilter(Class<?> targetType, String originalName, Method filterMethod) {
        if ("<init>".equals(originalName)) {
            throw new IllegalArgumentException(
                    "Filters on constructors are not supported: " + filterMethod);
        }
        Class<?> resultType = filterMethod.getReturnType();
        if (resultType == void.class) {
            throw new IllegalArgumentException(
                    "Filter method must return the original's (non-void) return type: " + filterMethod);
        }
        Class<?>[] filterParams = filterMethod.getParameterTypes();
        if (filterParams.length == 0 || filterParams[filterParams.length - 1] != resultType) {
            throw new IllegalArgumentException(
                    "Filter method's trailing parameter must match its return type: " + filterMethod);
        }

        boolean hasSelf = filterParams.length > 1 && filterParams[0] == targetType;
        Class<?>[] originalParams = Arrays.copyOfRange(
                filterParams, hasSelf ? 1 : 0, filterParams.length - 1);

        Class<?>[] originalSignature = Arrays.copyOf(originalParams, originalParams.length + 1);
        originalSignature[originalSignature.length - 1] = resultType;
        MethodDescriptor originalDesc = new MethodDescriptor(originalName, originalSignature);

        Class<?>[] signature = Arrays.copyOf(filterParams, filterParams.length + 1);
        signature[signature.length - 1] = resultType;
        MethodDescriptor filterDesc = new MethodDescriptor(filterMethod.getName(), signature);

        return new MethodDetour(originalDesc, hasSelf, filterDesc,
                filterMethod.getDeclaringClass(), filterMethod.getName());
    }

    /**
     * Builds a field hook mapping from a @FieldGet/@FieldSet method. Both are
     * filter-shaped over the field's value; the hook's return type doubles as
     * the expected field type, checked against each matched site.
     */
    private MethodDetour buildFieldHook(Kind kind, Class<?> targetType, String fieldName,
                                        Method hookMethod) {
        Class<?> valueType = hookMethod.getReturnType();
        if (valueType == void.class) {
            throw new IllegalArgumentException(
                    "Field hook must return the field's (non-void) type: " + hookMethod);
        }
        Class<?>[] params = hookMethod.getParameterTypes();
        if (params.length == 0 || params[params.length - 1] != valueType) {
            throw new IllegalArgumentException(
                    "Field hook's trailing parameter must match its return type: " + hookMethod);
        }
        boolean hasSelf = params.length == 2 && params[0] == targetType;
        if (params.length > 2 || (params.length == 2 && !hasSelf)) {
            throw new IllegalArgumentException(
                    "Field hook signature must be (T value) or (Target self, T value): " + hookMethod);
        }

        Class<?>[] signature = Arrays.copyOf(params, params.length + 1);
        signature[signature.length - 1] = valueType;
        MethodDescriptor hookDesc = new MethodDescriptor(hookMethod.getName(), signature);

        return new MethodDetour(kind, fieldName, new ValueType[]{ValueType.parse(valueType)},
                hasSelf, hookDesc, hookMethod.getDeclaringClass(), hookMethod.getName());
    }

    /**
     * Builds an element hook mapping from an @ElementGet/@ElementSet method:
     * {@code (Target self?, int index, T value) -> T}, where T is the array's
     * element type. Stored params hold the element type; the field's actual
     * array type is checked against {@code T[]} at every matched site.
     */
    private MethodDetour buildElementHook(Kind kind, Class<?> targetType, String fieldName,
                                          Method hookMethod) {
        Class<?> elementType = hookMethod.getReturnType();
        if (elementType == void.class) {
            throw new IllegalArgumentException(
                    "Element hook must return the array's (non-void) element type: " + hookMethod);
        }
        Class<?>[] params = hookMethod.getParameterTypes();
        if (params.length < 2
                || params[params.length - 1] != elementType
                || params[params.length - 2] != int.class) {
            throw new IllegalArgumentException(
                    "Element hook signature must be (int index, T value) or"
                            + " (Target self, int index, T value) with T matching the return type: "
                            + hookMethod);
        }
        boolean hasSelf = params.length == 3 && params[0] == targetType;
        if (params.length > 3 || (params.length == 3 && !hasSelf)) {
            throw new IllegalArgumentException(
                    "Element hook signature must be (int index, T value) or"
                            + " (Target self, int index, T value): " + hookMethod);
        }

        Class<?>[] signature = Arrays.copyOf(params, params.length + 1);
        signature[signature.length - 1] = elementType;
        MethodDescriptor hookDesc = new MethodDescriptor(hookMethod.getName(), signature);

        return new MethodDetour(kind, fieldName, new ValueType[]{ValueType.parse(elementType)},
                hasSelf, hookDesc, hookMethod.getDeclaringClass(), hookMethod.getName());
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
        // @DetourApplied probes live in the detour classes themselves, which
        // are typically @NoDetours - rewrite them before the opt-out below.
        if (registeredDetourClasses.contains(classHolder.getName())) {
            rewriteDetourAppliedProbes(classHolder);
        }

        // Allow opting out at the class level with @NoDetours
        if (classHolder.getAnnotations().get(NoDetours.class.getName()) != null) {
            return;
        }

        for (MethodHolder method : classHolder.getMethods()) {
            transformMethod(method);
        }
    }

    /**
     * Rewrites every @DetourApplied method of a registered detour class from
     * its mandatory {@code return false;} body to return true.
     * <p>
     * Edits the javac-produced constant in place instead of synthesizing a
     * replacement {@link Program}: hand-built IR has to honor renderer
     * invariants the model API doesn't enforce. The strict body convention is
     * what makes the in-place edit safe, so violations fail the build.
     */
    private void rewriteDetourAppliedProbes(ClassHolder classHolder) {
        for (MethodHolder method : classHolder.getMethods()) {
            if (method.getAnnotations().get(DetourApplied.class.getName()) == null) {
                continue;
            }
            if (!method.hasModifier(ElementModifier.STATIC)
                    || method.getResultType() != ValueType.BOOLEAN
                    || !method.hasProgram()) {
                throw new IllegalStateException(
                        "@DetourApplied method must be a static boolean method with a body: "
                                + classHolder.getName() + "." + method.getName());
            }

            boolean rewritten = false;
            for (BasicBlock block : method.getProgram().getBasicBlocks()) {
                for (Instruction insn : block) {
                    if (insn instanceof IntegerConstantInstruction) {
                        IntegerConstantInstruction constant = (IntegerConstantInstruction) insn;
                        if (constant.getConstant() == 0) {
                            constant.setConstant(1);
                            rewritten = true;
                        }
                    }
                }
            }
            if (!rewritten) {
                throw new IllegalStateException(
                        "@DetourApplied method body must be exactly 'return false;': "
                                + classHolder.getName() + "." + method.getName());
            }
            log.debug("Rewrote @DetourApplied probe {}.{} to true",
                    classHolder.getName(), method.getName());
        }
    }

    private void transformMethod(MethodHolder method) {
        if (!method.hasProgram()) {
            return;
        }

        Program program = method.getProgram();

        // Two-phase: collect first, then transform. Advice insertion and
        // guard block-splitting mutate the program, which must not happen
        // mid-iteration - and it keeps the loop from walking into freshly
        // inserted calls. Collected instructions stay valid across splits: an
        // instruction keeps its identity when it moves to a split-off block.
        boolean sawElementAccess = false;
        List<Instruction> targets = new ArrayList<>();
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction insn : block) {
                if (insn instanceof InvokeInstruction
                        || insn instanceof GetFieldInstruction
                        || insn instanceof PutFieldInstruction) {
                    targets.add(insn);
                } else if (hasElementHooks
                        && (insn instanceof GetElementInstruction
                                || insn instanceof PutElementInstruction)) {
                    targets.add(insn);
                    sawElementAccess = true;
                }
            }
        }

        // Array origins must be traced on the untransformed program: a
        // @FieldGet hook on the same field re-plumbs the GetField's receiver,
        // which would otherwise break the chain from field read to element
        // access. Variable identities survive those edits, so the map built
        // here stays valid.
        Map<Variable, FieldOrigin> origins =
                sawElementAccess ? buildFieldOrigins(program) : Collections.emptyMap();

        BasicBlockSplitter splitter = null;
        for (Instruction insn : targets) {
            if (insn instanceof InvokeInstruction) {
                splitter = transformInvoke(method, program, splitter, (InvokeInstruction) insn);
            } else if (insn instanceof GetFieldInstruction) {
                transformFieldGet(method, program, (GetFieldInstruction) insn);
            } else if (insn instanceof PutFieldInstruction) {
                transformFieldSet(method, program, (PutFieldInstruction) insn);
            } else if (insn instanceof GetElementInstruction) {
                transformElementGet(method, program, (GetElementInstruction) insn, origins);
            } else {
                transformElementSet(method, program, (PutElementInstruction) insn, origins);
            }
        }
        if (splitter != null) {
            splitter.fixProgram();
        }
    }

    /**
     * Maps each variable holding an array (or unwrapped array data) to the
     * field read it came from, following unwrap and plain-assignment links to
     * a fixpoint. Phi merges and values crossing method boundaries are
     * deliberately not traced — see the binding-scope note on
     * {@link ElementGet}.
     */
    private static Map<Variable, FieldOrigin> buildFieldOrigins(Program program) {
        Map<Variable, FieldOrigin> origins = new HashMap<>();
        List<Variable[]> aliases = new ArrayList<>();
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction insn : block) {
                if (insn instanceof GetFieldInstruction) {
                    GetFieldInstruction get = (GetFieldInstruction) insn;
                    if (get.getReceiver() != null) {
                        origins.put(get.getReceiver(),
                                new FieldOrigin(get.getField(), get.getInstance(), get.getFieldType()));
                    }
                } else if (insn instanceof UnwrapArrayInstruction) {
                    UnwrapArrayInstruction unwrap = (UnwrapArrayInstruction) insn;
                    aliases.add(new Variable[]{unwrap.getReceiver(), unwrap.getArray()});
                } else if (insn instanceof AssignInstruction) {
                    AssignInstruction assign = (AssignInstruction) insn;
                    aliases.add(new Variable[]{assign.getReceiver(), assign.getAssignee()});
                }
            }
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Variable[] alias : aliases) {
                if (!origins.containsKey(alias[0]) && origins.containsKey(alias[1])) {
                    origins.put(alias[0], origins.get(alias[1]));
                    changed = true;
                }
            }
        }
        return origins;
    }

    private void transformElementGet(MethodHolder owner, Program program, GetElementInstruction get,
                                     Map<Variable, FieldOrigin> origins) {
        FieldOrigin origin = origins.get(get.getArray());
        if (origin == null) {
            return;
        }
        MethodDetour[] detoursForClass = detours.get(origin.field.getClassName());
        if (detoursForClass == null) {
            return;
        }
        for (MethodDetour detour : detoursForClass) {
            if (detour.kind != Kind.ELEMENT_GET
                    || !callerAllowed(detour, owner)
                    || !elementMatches(detour, origin)) {
                continue;
            }
            Variable result = get.getReceiver();
            if (result == null) {
                continue;
            }
            Variable raw = program.createVariable();
            get.setReceiver(raw);

            InvokeInstruction call = new InvokeInstruction();
            call.setType(InvocationType.SPECIAL);
            call.setMethod(new MethodReference(detour.detourClass.getName(), detour.adviceDescriptor));
            List<Variable> args = new ArrayList<>();
            if (detour.adviceHasSelf) {
                args.add(origin.instance);
            }
            args.add(get.getIndex());
            args.add(raw);
            call.setArguments(args.toArray(new Variable[0]));
            call.setReceiver(result);
            call.setLocation(get.getLocation());
            get.insertNext(call);
            detour.matchedSites++;
        }
    }

    private void transformElementSet(MethodHolder owner, Program program, PutElementInstruction put,
                                     Map<Variable, FieldOrigin> origins) {
        FieldOrigin origin = origins.get(put.getArray());
        if (origin == null) {
            return;
        }
        MethodDetour[] detoursForClass = detours.get(origin.field.getClassName());
        if (detoursForClass == null) {
            return;
        }
        for (MethodDetour detour : detoursForClass) {
            if (detour.kind != Kind.ELEMENT_SET
                    || !callerAllowed(detour, owner)
                    || !elementMatches(detour, origin)) {
                continue;
            }
            Variable piped = program.createVariable();
            InvokeInstruction call = new InvokeInstruction();
            call.setType(InvocationType.SPECIAL);
            call.setMethod(new MethodReference(detour.detourClass.getName(), detour.adviceDescriptor));
            List<Variable> args = new ArrayList<>();
            if (detour.adviceHasSelf) {
                args.add(origin.instance);
            }
            args.add(put.getIndex());
            args.add(put.getValue());
            call.setArguments(args.toArray(new Variable[0]));
            call.setReceiver(piped);
            call.setLocation(put.getLocation());
            put.insertPrevious(call);
            put.setValue(piped);
            detour.matchedSites++;
        }
    }

    /**
     * Whether an element hook applies to a traced array origin. Field name
     * and instance-vs-static form must agree; a field whose array type then
     * disagrees with the hook's element type is a build error (type drift).
     */
    private static boolean elementMatches(MethodDetour hook, FieldOrigin origin) {
        if (!origin.field.getFieldName().equals(hook.adviceTargetName)) {
            return false;
        }
        if (hook.adviceHasSelf != (origin.instance != null)) {
            return false;
        }
        ValueType expected = ValueType.arrayOf(hook.adviceTargetParams[0]);
        if (!expected.equals(origin.fieldType)) {
            throw new IllegalStateException(
                    "Element hook type mismatch on " + origin.field + ": field is "
                            + origin.fieldType + " but " + hook.detourClass.getName() + "."
                            + hook.detourName + " expects " + expected);
        }
        return true;
    }

    private void transformFieldGet(MethodHolder owner, Program program, GetFieldInstruction get) {
        MethodDetour[] detoursForClass = detours.get(get.getField().getClassName());
        if (detoursForClass == null) {
            return;
        }
        for (MethodDetour detour : detoursForClass) {
            if (detour.kind != Kind.FIELD_GET
                    || !callerAllowed(detour, owner)
                    || !fieldMatches(detour, get.getField(), get.getInstance(), get.getFieldType())) {
                continue;
            }
            Variable result = get.getReceiver();
            if (result == null) {
                continue;
            }
            Variable raw = program.createVariable();
            get.setReceiver(raw);

            InvokeInstruction call = new InvokeInstruction();
            call.setType(InvocationType.SPECIAL);
            call.setMethod(new MethodReference(detour.detourClass.getName(), detour.adviceDescriptor));
            List<Variable> args = new ArrayList<>();
            if (detour.adviceHasSelf) {
                args.add(get.getInstance());
            }
            args.add(raw);
            call.setArguments(args.toArray(new Variable[0]));
            call.setReceiver(result);
            call.setLocation(get.getLocation());
            get.insertNext(call);
            detour.matchedSites++;
        }
    }

    private void transformFieldSet(MethodHolder owner, Program program, PutFieldInstruction put) {
        MethodDetour[] detoursForClass = detours.get(put.getField().getClassName());
        if (detoursForClass == null) {
            return;
        }
        for (MethodDetour detour : detoursForClass) {
            if (detour.kind != Kind.FIELD_SET
                    || !callerAllowed(detour, owner)
                    || !fieldMatches(detour, put.getField(), put.getInstance(), put.getFieldType())) {
                continue;
            }
            Variable piped = program.createVariable();
            InvokeInstruction call = new InvokeInstruction();
            call.setType(InvocationType.SPECIAL);
            call.setMethod(new MethodReference(detour.detourClass.getName(), detour.adviceDescriptor));
            List<Variable> args = new ArrayList<>();
            if (detour.adviceHasSelf) {
                args.add(put.getInstance());
            }
            args.add(put.getValue());
            call.setArguments(args.toArray(new Variable[0]));
            call.setReceiver(piped);
            call.setLocation(put.getLocation());
            put.insertPrevious(call);
            put.setValue(piped);
            detour.matchedSites++;
        }
    }

    /**
     * Whether a field hook applies to an access site. Name and
     * instance-vs-static form must agree; a site that then disagrees on the
     * field's type is a build error (type drift), never a silent skip.
     */
    private static boolean fieldMatches(MethodDetour hook, FieldReference field,
                                        Variable instance, ValueType fieldType) {
        if (!field.getFieldName().equals(hook.adviceTargetName)) {
            return false;
        }
        if (hook.adviceHasSelf != (instance != null)) {
            return false;
        }
        if (!fieldType.equals(hook.adviceTargetParams[0])) {
            throw new IllegalStateException(
                    "Field hook type mismatch on " + field + ": site has " + fieldType
                            + " but " + hook.detourClass.getName() + "." + hook.detourName
                            + " expects " + hook.adviceTargetParams[0]);
        }
        return true;
    }

    private BasicBlockSplitter transformInvoke(MethodHolder owner, Program program,
                                               BasicBlockSplitter splitter, InvokeInstruction invoke) {
        MethodReference ref = invoke.getMethod();
        MethodDetour[] detoursForClass = detours.get(ref.getClassName());
        if (detoursForClass == null) {
            return splitter;
        }

        List<MethodDetour> guards = new ArrayList<>();
        List<MethodDetour> finallies = new ArrayList<>();
        List<MethodDetour> advice = new ArrayList<>();
        List<MethodDetour> filters = new ArrayList<>();
        boolean anyReplace = false;
        for (MethodDetour detour : detoursForClass) {
            if (!callerAllowed(detour, owner)) {
                continue;
            }
            switch (detour.kind) {
                case GUARD:
                    if (adviceMatches(detour, invoke, ref)) {
                        guards.add(detour);
                    }
                    break;
                case FINALLY:
                    if (adviceMatches(detour, invoke, ref)) {
                        finallies.add(detour);
                    }
                    break;
                case BEFORE:
                case AFTER:
                    if (adviceMatches(detour, invoke, ref)) {
                        advice.add(detour);
                    }
                    break;
                case FILTER:
                    if (ref.getDescriptor().equals(detour.original)
                            && detour.adviceHasSelf == (invoke.getInstance() != null)) {
                        filters.add(detour);
                    }
                    break;
                case REPLACE:
                    anyReplace |= ref.getDescriptor().equals(detour.original);
                    break;
                default:
                    // field/element hooks anchor on field accesses, not calls
                    break;
            }
        }

        // A guard or @Finally owns the call site's control flow, so at most
        // one of either may bind, and a guard additionally excludes
        // replacements and filters. Wrap the control flow FIRST, so advice
        // and filters inserted next to the invoke land inside the guarded /
        // protected block.
        if (!guards.isEmpty() || !finallies.isEmpty()) {
            if (guards.size() > 1 || finallies.size() > 1
                    || (!guards.isEmpty() && !finallies.isEmpty())
                    || (!guards.isEmpty() && (anyReplace || !filters.isEmpty()))) {
                throw new IllegalStateException(
                        "Conflicting detours on call site " + ref + " in "
                                + owner.getOwnerName() + "." + owner.getName()
                                + ": at most one guard or @Finally may bind a site, and a guard"
                                + " cannot combine with a replacement or a filter");
            }
            if (splitter == null) {
                splitter = new BasicBlockSplitter(program);
            }
            if (!guards.isEmpty()) {
                applyGuard(program, splitter, guards.get(0), invoke);
                guards.get(0).matchedSites++;
            } else {
                applyFinally(program, splitter, finallies.get(0), invoke);
                finallies.get(0).matchedSites++;
            }
        }

        // Advice next, while the instruction still reflects the original call
        // - a replacement below rewrites the receiver/args/method in place.
        for (MethodDetour detour : advice) {
            insertAdvice(detour, invoke);
            detour.matchedSites++;
        }

        // Filters wrap the call's result; they compose with a replacement
        // (the filter call pipes whatever the rewritten invoke produces).
        for (MethodDetour detour : filters) {
            applyFilter(program, detour, invoke);
            detour.matchedSites++;
        }

        for (MethodDetour detour : detoursForClass) {
            if (detour.kind != Kind.REPLACE || !callerAllowed(detour, owner)) {
                continue;
            }
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
                detour.matchedSites++;

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

            detour.matchedSites++;
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
                // The original was a VIRTUAL invoke; a static call must be
                // SPECIAL. Leaving it VIRTUAL with a null instance is invalid
                // IR that NPEs TeaVM's optimizer (InvokeInstruction
                // .getInstance() is null) at BALANCED/AGGRESSIVE.
                invoke.setType(InvocationType.SPECIAL);

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

        return splitter;
    }

    /**
     * Whether a detour's {@link Callers} restriction (if any) admits sites
     * inside the given method's class.
     */
    private static boolean callerAllowed(MethodDetour detour, MethodHolder owner) {
        return detour.callers == null || detour.callers.contains(owner.getOwnerName());
    }

    /**
     * Whether an advice mapping applies to a call site. Matched on method
     * name, parameter types and instance-vs-static form; the original's
     * return type deliberately does not participate (see {@link Before}).
     */
    private static boolean adviceMatches(MethodDetour advice, InvokeInstruction invoke,
                                         MethodReference ref) {
        if (!ref.getName().equals(advice.adviceTargetName)) {
            return false;
        }
        if (advice.adviceHasSelf != (invoke.getInstance() != null)) {
            return false;
        }
        return Arrays.equals(ref.getDescriptor().getParameterTypes(), advice.adviceTargetParams);
    }

    /**
     * Inserts the advice call adjacent to the original invoke, reusing the
     * call site's own receiver/argument variables. @After advice lands
     * immediately after the invoke, so it runs only on normal completion -
     * an exception skips it exactly like the statement following the call.
     */
    private static void insertAdvice(MethodDetour advice, InvokeInstruction invoke) {
        InvokeInstruction call = buildAdviceCall(advice, invoke);
        if (advice.kind == Kind.BEFORE) {
            invoke.insertPrevious(call);
        } else {
            invoke.insertNext(call);
        }
    }

    /** A static call to an advice-shaped hook, with the invoke's receiver/args. */
    private static InvokeInstruction buildAdviceCall(MethodDetour advice, InvokeInstruction invoke) {
        InvokeInstruction call = new InvokeInstruction();
        call.setType(InvocationType.SPECIAL);
        call.setMethod(new MethodReference(advice.detourClass.getName(), advice.adviceDescriptor));

        List<Variable> args = new ArrayList<>();
        if (advice.adviceHasSelf) {
            args.add(invoke.getInstance());
        }
        args.addAll(invoke.getArguments());
        call.setArguments(args.toArray(new Variable[0]));
        call.setLocation(invoke.getLocation());
        return call;
    }

    /**
     * Wraps a call site so the advice runs on both exits:
     *
     * <pre>
     * head:    ...           -> jump call
     * call:    [try: catch-all -> handler] result = original invoke;
     *          hook(self?, args...); jump join
     * handler: hook(self?, args...); rethrow      (covered by the site's
     *                                              original handlers, so the
     *                                              rethrow stays catchable)
     * join:    ...rest...
     * </pre>
     *
     * The synthesized catch-all sits ahead of the handlers the split copied
     * onto the call block, so it intercepts first; no phi is needed because
     * the exception path rethrows and never reaches the join. The handler
     * defines nothing but its exception variable, which keeps any enclosing
     * handler's phi wiring untouched.
     */
    private static void applyFinally(Program program, BasicBlockSplitter splitter,
                                     MethodDetour advice, InvokeInstruction invoke) {
        BasicBlock headBlock = invoke.getBasicBlock();
        TextLocation location = invoke.getLocation();

        // getPrevious() == null (invoke first in block) is fine: the splitter
        // treats a null split point as "move every instruction".
        BasicBlock invokeBlock = splitter.split(headBlock, invoke.getPrevious());
        BasicBlock joinBlock = splitter.split(invokeBlock, invoke);

        BasicBlock handlerBlock = program.createBasicBlock();
        Variable exception = program.createVariable();
        handlerBlock.setExceptionVariable(exception);
        handlerBlock.getTryCatchBlocks().addAll(ProgramUtils.copyTryCatches(headBlock, program));

        handlerBlock.add(buildAdviceCall(advice, invoke));

        RaiseInstruction rethrow = new RaiseInstruction();
        rethrow.setException(exception);
        rethrow.setLocation(location);
        handlerBlock.add(rethrow);

        TryCatchBlock tryCatch = new TryCatchBlock();
        // no exception type set: catch everything
        tryCatch.setHandler(handlerBlock);
        invokeBlock.getTryCatchBlocks().add(0, tryCatch);

        invoke.insertNext(buildAdviceCall(advice, invoke));

        JumpInstruction headJump = new JumpInstruction();
        headJump.setTarget(invokeBlock);
        headJump.setLocation(location);
        headBlock.add(headJump);

        JumpInstruction invokeJump = new JumpInstruction();
        invokeJump.setTarget(joinBlock);
        invokeJump.setLocation(location);
        invokeBlock.add(invokeJump);
    }

    /**
     * Wraps a guarded call site in a conditional:
     *
     * <pre>
     * head:    ... ctx = new Interception(); guard(self?, args..., ctx);
     *          flag = ctx.isCancelled(); if (flag != 0) goto skip else goto call
     * call:    result1 = original invoke; goto join
     * skip:    result2 = ctx.getX() [+ cast]; goto join
     * join:    result = phi(result1@call, result2@skip); ...rest...
     * </pre>
     *
     * Split mechanics: a block may only be split once, but a split-off tail
     * may be split again — so the head block is split after the prep
     * instructions (moving the invoke and the rest out) and the resulting
     * tail is split after the invoke (leaving the invoke alone in its block).
     * The hand-built phi is safe from {@link BasicBlockSplitter#fixProgram()}:
     * its incomings reference blocks created after the splitter initialized,
     * which fixProgram explicitly ignores.
     */
    private static void applyGuard(Program program, BasicBlockSplitter splitter,
                                   MethodDetour guard, InvokeInstruction invoke) {
        String interception = Interception.class.getName();
        MethodReference ref = invoke.getMethod();
        BasicBlock headBlock = invoke.getBasicBlock();
        TextLocation location = invoke.getLocation();

        // ctx = new Interception()
        Variable ctx = program.createVariable();
        ConstructInstruction construct = new ConstructInstruction();
        construct.setType(interception);
        construct.setReceiver(ctx);
        construct.setLocation(location);
        invoke.insertPrevious(construct);

        InvokeInstruction init = new InvokeInstruction();
        init.setType(InvocationType.SPECIAL);
        init.setInstance(ctx);
        init.setMethod(new MethodReference(interception, new MethodDescriptor("<init>", void.class)));
        init.setLocation(location);
        invoke.insertPrevious(init);

        // guard(self?, args..., ctx)
        InvokeInstruction guardCall = new InvokeInstruction();
        guardCall.setType(InvocationType.SPECIAL);
        guardCall.setMethod(new MethodReference(guard.detourClass.getName(), guard.adviceDescriptor));
        List<Variable> args = new ArrayList<>();
        if (guard.adviceHasSelf) {
            args.add(invoke.getInstance());
        }
        args.addAll(invoke.getArguments());
        args.add(ctx);
        guardCall.setArguments(args.toArray(new Variable[0]));
        guardCall.setLocation(location);
        invoke.insertPrevious(guardCall);

        // flag = ctx.isCancelled()
        Variable flag = program.createVariable();
        InvokeInstruction cancelledCall = new InvokeInstruction();
        cancelledCall.setType(InvocationType.SPECIAL);
        cancelledCall.setInstance(ctx);
        cancelledCall.setMethod(new MethodReference(interception,
                new MethodDescriptor("isCancelled", boolean.class)));
        cancelledCall.setReceiver(flag);
        cancelledCall.setLocation(location);
        invoke.insertPrevious(cancelledCall);

        BasicBlock invokeBlock = splitter.split(headBlock, cancelledCall);
        BasicBlock joinBlock = splitter.split(invokeBlock, invoke);

        BasicBlock skipBlock = program.createBasicBlock();
        skipBlock.getTryCatchBlocks().addAll(ProgramUtils.copyTryCatches(headBlock, program));

        // Merge the two paths' results when the call site uses the value.
        Variable result = invoke.getReceiver();
        if (result != null) {
            Variable invokeResult = program.createVariable();
            invoke.setReceiver(invokeResult);

            ValueType returnType = ref.getDescriptor().getResultType();
            Variable skipResult;
            InvokeInstruction accessor = new InvokeInstruction();
            accessor.setType(InvocationType.SPECIAL);
            accessor.setInstance(ctx);
            accessor.setLocation(location);
            if (returnType instanceof ValueType.Primitive) {
                accessor.setMethod(new MethodReference(interception,
                        primitiveAccessor((ValueType.Primitive) returnType)));
                skipResult = program.createVariable();
                accessor.setReceiver(skipResult);
                skipBlock.add(accessor);
            } else {
                accessor.setMethod(new MethodReference(interception,
                        new MethodDescriptor("getObject", Object.class)));
                Variable raw = program.createVariable();
                accessor.setReceiver(raw);
                skipBlock.add(accessor);

                skipResult = program.createVariable();
                CastInstruction cast = new CastInstruction();
                cast.setValue(raw);
                cast.setReceiver(skipResult);
                cast.setTargetType(returnType);
                cast.setLocation(location);
                skipBlock.add(cast);
            }

            Phi phi = new Phi();
            phi.setReceiver(result);
            Incoming fromInvoke = new Incoming();
            fromInvoke.setSource(invokeBlock);
            fromInvoke.setValue(invokeResult);
            phi.getIncomings().add(fromInvoke);
            Incoming fromSkip = new Incoming();
            fromSkip.setSource(skipBlock);
            fromSkip.setValue(skipResult);
            phi.getIncomings().add(fromSkip);
            joinBlock.getPhis().add(phi);
        }

        JumpInstruction invokeJump = new JumpInstruction();
        invokeJump.setTarget(joinBlock);
        invokeJump.setLocation(location);
        invokeBlock.add(invokeJump);

        JumpInstruction skipJump = new JumpInstruction();
        skipJump.setTarget(joinBlock);
        skipJump.setLocation(location);
        skipBlock.add(skipJump);

        BranchingInstruction branch = new BranchingInstruction(BranchingCondition.NOT_EQUAL);
        branch.setOperand(flag);
        branch.setConsequent(skipBlock);
        branch.setAlternative(invokeBlock);
        branch.setLocation(location);
        headBlock.add(branch);
    }

    private static MethodDescriptor primitiveAccessor(ValueType.Primitive type) {
        switch (type.getKind()) {
            case BOOLEAN:
                return new MethodDescriptor("getBoolean", boolean.class);
            case BYTE:
                return new MethodDescriptor("getByte", byte.class);
            case SHORT:
                return new MethodDescriptor("getShort", short.class);
            case CHARACTER:
                return new MethodDescriptor("getChar", char.class);
            case INTEGER:
                return new MethodDescriptor("getInt", int.class);
            case LONG:
                return new MethodDescriptor("getLong", long.class);
            case FLOAT:
                return new MethodDescriptor("getFloat", float.class);
            case DOUBLE:
                return new MethodDescriptor("getDouble", double.class);
            default:
                throw new IllegalStateException("Unexpected primitive kind: " + type.getKind());
        }
    }

    /**
     * Pipes the call's result through the filter: the invoke gets a fresh
     * receiver, and the filter call consumes it and defines the variable the
     * rest of the method already reads. A call site that discards the value
     * still runs the filter (for its side effects); its result is discarded
     * too. Multiple filters chain, innermost = last registered.
     */
    private static void applyFilter(Program program, MethodDetour filter, InvokeInstruction invoke) {
        Variable result = invoke.getReceiver();
        Variable piped = program.createVariable();
        invoke.setReceiver(piped);

        InvokeInstruction call = new InvokeInstruction();
        call.setType(InvocationType.SPECIAL);
        call.setMethod(new MethodReference(filter.detourClass.getName(), filter.adviceDescriptor));
        List<Variable> args = new ArrayList<>();
        if (filter.adviceHasSelf) {
            args.add(invoke.getInstance());
        }
        args.addAll(invoke.getArguments());
        args.add(piped);
        call.setArguments(args.toArray(new Variable[0]));
        call.setReceiver(result);
        call.setLocation(invoke.getLocation());
        invoke.insertNext(call);
    }

    /**
     * Build-time safety net: a {@link DependencyListener} that, once
     * dependency analysis completes (i.e. every reachable class has been
     * transformed), reports every registered detour that matched zero call
     * sites. A silent zero-match is how a renamed original or a drifted
     * signature quietly un-hooks a detour.
     * <p>
     * Register it alongside the transformer itself:
     * <pre>
     * DetourHacks detours = ...;
     * host.add(detours);
     * host.add(detours.zeroMatchVerifier(true));
     * </pre>
     *
     * @param failOnUnmatched true to report unmatched detours as build
     *                        errors — right for an application's own detours,
     *                        where every registration is expected to bind.
     *                        Use false (warnings) for opportunistic library
     *                        detour sets whose target APIs an application may
     *                        legitimately never use. Intentionally dead
     *                        detours should be annotated {@link DisableDetour}
     *                        rather than left unmatched.
     */
    public DependencyListener zeroMatchVerifier(boolean failOnUnmatched) {
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
                detours.forEach((targetClass, detourList) -> {
                    for (MethodDetour detour : detourList) {
                        if (detour.matchedSites > 0) {
                            continue;
                        }
                        String originalName = detour.original != null
                                ? detour.original.getName()
                                : detour.adviceTargetName;
                        String message = "Detour matched no call sites: " + detour.kind + " on "
                                + targetClass + "." + originalName + " -> "
                                + detour.detourClass.getName() + "." + detour.detourName
                                + " (was the original renamed? annotate with @DisableDetour if"
                                + " intentionally disabled)";
                        if (failOnUnmatched) {
                            agent.getDiagnostics().error(null, message);
                        } else {
                            agent.getDiagnostics().warning(null, message);
                        }
                    }
                });
            }

            @Override
            public void complete() {
            }
        };
    }
}
