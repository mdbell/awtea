package me.mdbell.awtea.gl;

import me.mdbell.awtea.util.ClasspathRoot;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.StringConstantInstruction;

import java.io.IOException;

import static me.mdbell.awtea.util.ShaderIncludeProcessor.preprocessIncludes;

/**
 * Embeds include-preprocessed shader sources into {@code @ShaderSource}
 * methods at build time by rewriting the placeholder string constant in their
 * IR — the annotated method must be a static String method whose body is
 * exactly {@code return "";}. This is the shader-specific sibling of
 * awtea-instrument's generic {@code @EmbedResource}: same mechanism, plus
 * shader include expansion.
 * <p>
 * This replaces the old {@code EmbedGenerator}, which emitted JS source
 * directly ({@code return $rt_str("...")}). Hand-emitted JS hard-codes
 * runtime helper names and its own string escaping, both of which break under
 * obfuscation; a rewritten IR constant is an ordinary pooled Java string that
 * every backend, optimization level and obfuscation setting handles natively.
 * The in-place constant edit (rather than a synthesized program) follows the
 * same rule as awtea-instrument's {@code @BuildConstant}: hand-built IR has
 * to honor renderer invariants the model API doesn't enforce.
 * <p>
 * The placeholder is also the JVM-side behavior: off TeaVM the methods just
 * return the empty string instead of the old {@code native} methods'
 * {@code UnsatisfiedLinkError}.
 */
public class EmbedTransformer implements ClassHolderTransformer {

    private static final Logger log = LoggerFactory.getLogger(EmbedTransformer.class);

    private final ClassLoader classLoader;

    public EmbedTransformer(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void transformClass(ClassHolder classHolder, ClassHolderTransformerContext context) {
        for (MethodHolder method : classHolder.getMethods()) {
            AnnotationHolder shader = method.getAnnotations().get(ShaderSource.class.getName());
            if (shader == null) {
                continue;
            }
            String location = classHolder.getName() + "." + method.getName();

            String path = shader.getValue("value").getString();
            String text;
            try {
                text = preprocessIncludes(path, new ClasspathRoot(classLoader));
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Failed to read embedded shader '" + path + "' for " + location, e);
            }

            if (!method.hasModifier(ElementModifier.STATIC)
                    || !ValueType.parse(String.class).equals(method.getResultType())
                    || !method.hasProgram()) {
                throw new IllegalStateException(
                        "@ShaderSource method must be a static String method with a"
                                + " 'return \"\";' placeholder body (not native): " + location);
            }

            int replaced = 0;
            for (BasicBlock block : method.getProgram().getBasicBlocks()) {
                for (Instruction insn : block) {
                    if (insn instanceof StringConstantInstruction) {
                        ((StringConstantInstruction) insn).setConstant(text);
                        replaced++;
                    }
                }
            }
            if (replaced != 1) {
                throw new IllegalStateException(
                        "@ShaderSource method body must be exactly 'return \"\";'"
                                + " (found " + replaced + " string constants): " + location);
            }
            log.debug("Embedded '{}' into {} ({} chars)", path, location, text.length());
        }
    }
}
