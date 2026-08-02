package me.mdbell.awtea.classlib;

import org.teavm.extension.spi.substitution.SimpleSubstitutionPolicy;
import org.teavm.extension.spi.substitution.SubstitutionSink;

/**
 * Maps the {@code java.*}/{@code javax.*} classes awtea implements onto their
 * {@code me.mdbell.awtea.classlib.*} T-prefixed implementations. Replaces the
 * {@code META-INF/teavm.properties} declaration TeaVM 0.15 removed:
 *
 * <pre>
 * mapPackageHierarchy|me.mdbell.awtea.classlib.java=java
 * mapPackageHierarchy|me.mdbell.awtea.classlib.javax=javax
 * stripPrefixFromPackageHierarchyClasses|me.mdbell.awtea.classlib=T
 * includePackageHierarchy|java.sound=false
 * </pre>
 *
 * Unlike the old properties, the new SPI declares rules in the
 * original-to-substitute direction and resolves them with an existence check,
 * so this rule coexists with TeaVM's own {@code java.** -> org.teavm.classlib}
 * mapping: whichever substitute class actually exists on the classpath wins.
 *
 * Registered through {@code META-INF/services} directly (rather than the
 * {@code @Autoregistered} processor) so the build needs no extra annotation
 * processor.
 */
public class AwteaSubstitutionPolicy extends SimpleSubstitutionPolicy {
    @Override
    public void contribute(SubstitutionSink sink) {
        sink.selectClasses(inPackage("java", true).or(inPackage("javax", true)))
                .packagePrefix("me.mdbell.awtea.classlib.")
                .simpleNamePrefix("T");
        sink.selectClasses(inPackage("java.sound", true)).dontFallbackWhenNoSubstitution();
    }
}
