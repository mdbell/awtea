package me.mdbell.awtea.util;

import java.io.IOException;
import java.io.InputStream;

public class ClasspathRoot implements PathRoot {

    private final ClassLoader loader;

    public ClasspathRoot(ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public String read(String relativePath) throws IOException {
        String normalized = normalize(relativePath);
        try (InputStream is = loader.getResourceAsStream(normalized)) {
            if (is == null) {
                throw new IOException("Resource not found in classpath: " + normalized);
            }
            return new String(is.readAllBytes());
        }
    }
}

