package me.mdbell.awtea.detour;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DetourMethod {
	/**
	 * Original method name. For constructors use "<init>".
	 * If empty, the original name is assumed to be the same as the detour method name.
	 */
	String value() default "";

	boolean constructor() default false;
}
