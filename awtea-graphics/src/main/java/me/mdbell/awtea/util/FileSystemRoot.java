package me.mdbell.awtea.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSystemRoot implements PathRoot {

    private final Path rootDir;

    public FileSystemRoot(Path rootDir) {
        this.rootDir = rootDir.toAbsolutePath().normalize();
    }

    @Override
    public String read(String relativePath) throws IOException {
        Path file = rootDir.resolve(normalize(relativePath));
        file = file.toAbsolutePath().normalize();

        if (!file.startsWith(rootDir)) {
            throw new IOException("Access outside root directory is not allowed: " + file);
        }

        return Files.readString(file);
    }
}
