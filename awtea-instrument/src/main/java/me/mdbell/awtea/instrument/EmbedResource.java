package me.mdbell.awtea.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Embeds a classpath resource as a String at build time: the annotated
 * method must be a static String method whose body is exactly
 * {@code return "";}, and {@link EmbedResourceTransformer} rewrites the
 * placeholder constant to the resource's UTF-8 contents. The embedded text
 * is an ordinary pooled string, safe under every optimization and
 * obfuscation setting.
 * <p>
 * The placeholder doubles as the off-TeaVM behavior (plain JVM runs return
 * the empty string). Resources are loaded verbatim; domain-specific
 * preprocessing belongs in specialized annotations (e.g. awtea-graphics'
 * {@code @ShaderSource}, which runs shader include expansion).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EmbedResource {
    /** Classpath resource path to embed. */
    String value();
}
