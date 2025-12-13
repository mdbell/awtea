package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MonitorReferencePolicy {

	Policy value() default Policy.WEAK;

	enum Policy {
		STRONG,
		WEAK
	}
}
