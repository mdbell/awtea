package me.mdbell.awtea.gfx.gl;

import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

public class EmbedGenerator implements Generator {

	@Override
	public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) {
		Method javaMethod = findJavaMethod(context.getClassLoader(), methodRef);
		ShaderSource ann = javaMethod.getAnnotation(ShaderSource.class);
		if (ann == null) {
			throw new RuntimeException("@ShaderSource missing on " + methodRef);
		}

		String path = ann.value();

		// 2) Load the file contents from the classpath
		String shaderText = loadResourceAsString(context.getClassLoader(), path);

		// 3) Emit JS: return "<shaderText>";
		writer.append("return $rt_str(");
		try {
			emitStringLiteral(writer, shaderText);
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
				throw new RuntimeException("Shader resource not found on classpath: " + path);
			}
			byte[] bytes = in.readAllBytes();
			return new String(bytes, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("Error reading shader resource: " + path, e);
		}
	}

}
