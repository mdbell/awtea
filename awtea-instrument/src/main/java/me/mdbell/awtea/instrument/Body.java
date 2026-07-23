package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method-body detour: instead of rewriting call sites, {@link DetourHacks}
 * replaces the target method's <em>body</em> with a delegation to the hook,
 * preserving the original body as a synthetic sibling method. Every
 * invocation path is therefore hooked — virtual dispatch through a supertype
 * reference, interface calls, reflection, and calls made inside
 * {@code @NoDetours} classes — which call-site rewriting cannot see.
 * <p>
 * Signature conventions are those of {@link DetourMethod}: the hook is
 * {@code public static}, returns the original's return type, and takes a
 * leading {@code Target self} parameter for instance methods.
 * <p>
 * Calling through: inside the hook's <em>declaring class only</em>, calls to
 * the original method are rewritten to the preserved body, so
 * {@code Target.foo(...)}/{@code self.foo(...)} from the hook (or its private
 * helpers) reaches the real implementation. Anywhere else — including other
 * {@code @NoDetours} detour classes — the same call lands in the hook; that
 * is the point of a body detour.
 * <p>
 * Sharp edges:
 * <ul>
 * <li>Only the named class's body is replaced. A subclass override is a
 * different body and is not hooked — annotate it separately if needed.</li>
 * <li>Constructors and native/abstract methods are not supported.</li>
 * <li>{@link Callers} cannot apply (there is no call site); combining them
 * is a build error. Call-site detours on the same method still bind their
 * sites and compose in front of the body hook.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Body {
    /**
     * Original method name. If empty, the original name is assumed to be the
     * same as the hook method name.
     */
    String value() default "";
}
