package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Advice variant of {@link DetourMethod}: instead of replacing calls to the
 * original method, {@link DetourHacks} inserts a call to this method
 * immediately after every matched call site, leaving the original call in
 * place. The advice runs only when the original completes normally — an
 * exception skips it, exactly as it would skip the statement following the
 * call.
 * <p>
 * Signature and matching conventions are identical to {@link Before}. The
 * original's return value is not passed to the advice.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface After {
    /**
     * Original method name. If empty, the original name is assumed to be the
     * same as the advice method name.
     */
    String value() default "";
}
