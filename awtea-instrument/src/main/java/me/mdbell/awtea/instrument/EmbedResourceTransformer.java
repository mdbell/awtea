package me.mdbell.awtea.instrument;

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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Rewrites {@link EmbedResource} placeholder constants to the resource's
 * contents — the flip-a-constant mechanism shared with
 * {@link BuildConstants}, with the value loaded from the classpath instead
 * of supplied by the build plugin. Registered by
 * {@code CustomTransformersPlugin}, so every awtea build has it. Violations
 * of the placeholder convention and missing resources fail the build.
 */
public class EmbedResourceTransformer implements ClassHolderTransformer {

    private static final Logger log = LoggerFactory.getLogger(EmbedResourceTransformer.class);

    private final ClassLoader classLoader;

    public EmbedResourceTransformer(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void transformClass(ClassHolder classHolder, ClassHolderTransformerContext context) {
        for (MethodHolder method : classHolder.getMethods()) {
            AnnotationHolder annotation =
                    method.getAnnotations().get(EmbedResource.class.getName());
            if (annotation == null) {
                continue;
            }
            String location = classHolder.getName() + "." + method.getName();
            String path = annotation.getValue("value").getString();

            String text;
            try (InputStream in = classLoader.getResourceAsStream(path)) {
                if (in == null) {
                    throw new IllegalStateException(
                            "@EmbedResource resource not found on classpath: '" + path
                                    + "' for " + location);
                }
                text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Failed to read @EmbedResource '" + path + "' for " + location, e);
            }

            if (!method.hasModifier(ElementModifier.STATIC)
                    || !ValueType.parse(String.class).equals(method.getResultType())
                    || !method.hasProgram()) {
                throw new IllegalStateException(
                        "@EmbedResource method must be a static String method with a"
                                + " 'return \"\";' placeholder body: " + location);
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
                        "@EmbedResource method body must be exactly 'return \"\";'"
                                + " (found " + replaced + " string constants): " + location);
            }
            log.debug("Embedded '{}' into {} ({} chars)", path, location, text.length());
        }
    }
}
