package me.mdbell.awtea.gl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ShaderSource {
	/**
	 * Classpath resource path to the shader file.
	 */
	String value();
}
