package me.mdbell.awtea.gl;

import me.mdbell.awtea.util.ClasspathRoot;
import me.mdbell.awtea.util.PathRoot;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static me.mdbell.awtea.util.ShaderIncludeProcessor.preprocessIncludes;

public class EmbedGenerator implements Generator {

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) {
        Method javaMethod = findJavaMethod(context.getClassLoader(), methodRef);

        // Check for @ShaderSource first
        ShaderSource shaderAnn = javaMethod.getAnnotation(ShaderSource.class);
        CSSSource cssAnn = javaMethod.getAnnotation(CSSSource.class);

        String path;
        if (shaderAnn != null) {
            path = shaderAnn.value();
        } else if (cssAnn != null) {
            path = cssAnn.value();
        } else {
            throw new RuntimeException("@ShaderSource or @CSSSource missing on " + methodRef);
        }

        try {

            ClassLoader loader = context.getClassLoader();

            // 2) Load the file contents from the classpath
            String resourceText;

            if (shaderAnn != null) {
                PathRoot root = new ClasspathRoot(loader);
                resourceText = preprocessIncludes(path, root);
            } else {
                resourceText = loadResourceAsString(loader, path);
            }

            // 3) Emit JS: return "<resourceText>";
            writer.append("return $rt_str(");
            emitStringLiteral(writer, resourceText);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        writer.append(");").softNewLine();
    }

    private void emitStringLiteral(SourceWriter writer, String s) throws IOException {
        writer.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                    writer.append("\\\\");
                    break;
                case '"':
                    writer.append("\\\"");
                    break;
                case '\n':
                    writer.append("\\n");
                    break;
                case '\r':
                    writer.append("\\r");
                    break;
                case '\t':
                    writer.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        // control chars
                        writer.append("\\u00");
                        String hex = Integer.toHexString(c);
                        if (hex.length() == 1) {
                            writer.append('0');
                        }
                        writer.append(hex);
                    } else {
                        writer.append(c);
                    }
            }
        }
        writer.append('"');
    }


    private Method findJavaMethod(ClassLoader cl, MethodReference ref) {
        String ownerName = ref.getClassName().replace('/', '.');
        try {
            Class<?> owner = Class.forName(ownerName, false, cl);
            MethodDescriptor desc = ref.getDescriptor();

            for (Method m : owner.getDeclaredMethods()) {
                if (!m.getName().equals(ref.getName())) {
                    continue;
                }
                if (m.getParameterCount() != desc.parameterCount()) {
                    continue;
                }
                return m;
            }
            throw new RuntimeException("Method not found via reflection: " + ref);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to load class for " + ref, e);
        }
    }

    private String loadResourceAsString(ClassLoader cl, String path) {
        try (InputStream in = cl.getResourceAsStream(path)) {
            if (in == null) {
                throw new RuntimeException("Resource not found on classpath: " + path);
            }
            byte[] bytes = in.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error reading resource: " + path, e);
        }
    }

}
