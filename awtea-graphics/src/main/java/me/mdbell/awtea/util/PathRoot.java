package me.mdbell.awtea.util;

import java.io.IOException;

public interface PathRoot {

    /**
     * Read the content of a relative file path.
     *
     * @param relativePath relative path from this root
     * @return file contents as string
     * @throws IOException if the file cannot be read
     */
    String read(String relativePath) throws IOException;

    /**
     * Get the path separator used by this PathRoot.
     *
     * @return The path separator string
     */
    default String separator() {
        return "/";
    }

    default String join(String... parts) {
        String sep = separator();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    /**
     * Normalize a path, resolving "." and ".." segments.
     *
     * @param path The input path
     * @return The normalized path
     */
    default String normalize(String path) {
        String sep = separator();
        String[] segments = path.split(sep.equals("/") ? "/" : "\\\\" + sep);
        StringBuilder sb = new StringBuilder();
        for (String segment : segments) {
            if (segment.isEmpty() || segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                int lastSepIndex = sb.lastIndexOf(sep);
                if (lastSepIndex != -1) {
                    sb.delete(lastSepIndex, sb.length());
                }
                continue;
            }
            if (sb.length() > 0) {
                sb.append(sep);
            }
            sb.append(segment);

        }
        return sb.toString();
    }

    /**
     * Resolve a relative path against a "parent" path.
     * This is optional and used for nested includes.
     */
    default String resolve(String parentPath, String relativePath) {
        if (parentPath == null || parentPath.isEmpty()) {
            return relativePath;
        }
        String sep = separator();
        int lastSepIndex = parentPath.lastIndexOf(sep);
        String basePath = (lastSepIndex != -1) ? parentPath.substring(0, lastSepIndex + 1) : "";
        return normalize(basePath + relativePath);
    }
}
