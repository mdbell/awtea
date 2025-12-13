package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Monitored {
	String value() default ""; // optional logical name

	/**
	 * Defines how method name is recorded in monitoring data.
	 * The rules are as follows:
	 * <ul>
	 *     <li>DEFAULT - use the default rule defined at class level (if any) or SIMPLE if no class-level rule is defined</li>
	 *     <li>SIMPLE - record only the method name without parameter types</li>
	 *     <li>FULL - record the full method signature including parameter types</li>
	 * </ul>
	 *
	 * @return the rule to apply for method name recording
	 */
	MethodNameRule rule() default MethodNameRule.DEFAULT;

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface Disabled {

	}

	@interface AllMethods {
		MethodNameRule defaultRule() default MethodNameRule.FULL;
	}

	enum MethodNameRule {
		DEFAULT,
		SIMPLE,
		FULL
	}
}
