package me.mdbell.awtea.util;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ShaderIncludeProcessor {

    /**
     * Preprocess a shader source, resolving #include directives.
     *
     * @param shaderSource Original GLSL code
     * @param root         PathRoot backend (disk, classpath, etc.)
     * @return Processed GLSL with includes inlined
     * @throws IOException if any included file cannot be read
     */
    public static String preprocessIncludes(String shaderPath, PathRoot root) throws IOException {
        // internally reads root.read(shaderPath)
        return preprocessIncludes(shaderPath, root, new HashSet<>(), shaderPath);
    }

    private static String preprocessIncludes(
            String shaderPath,
            PathRoot root,
            Set<String> alreadyIncluded,
            String parentPath
    ) throws IOException {
        // read contents of shaderPath
        String shaderSource = root.read(shaderPath);
        StringBuilder result = new StringBuilder();

        for (String line : shaderSource.split("\\R")) {
            line = line.trim();
            if (line.startsWith("#include")) {
                int start = line.indexOf('"');
                int end = line.lastIndexOf('"');
                if (start >= 0 && end > start) {
                    String includePath = line.substring(start + 1, end);
                    String resolvedPath = root.resolve(parentPath, includePath);

                    if (alreadyIncluded.contains(resolvedPath)) {
                        // Prevent circular includes
                        continue;
                    }
                    alreadyIncluded.add(resolvedPath);

                    String includedSource = preprocessIncludes(resolvedPath, root, alreadyIncluded, resolvedPath);

                    result.append(includedSource).append("\n");
                } else {
                    throw new IOException("Malformed #include directive: " + line);
                }
            } else {
                result.append(line).append("\n");
            }
        }

        return result.toString();
    }
}

