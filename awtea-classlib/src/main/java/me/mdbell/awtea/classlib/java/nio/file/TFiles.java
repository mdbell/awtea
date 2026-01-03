package me.mdbell.awtea.classlib.java.nio.file;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public class TFiles {

    private static final Logger log = LoggerFactory.getLogger(TFiles.class);

    public static BufferedReader newBufferedReader(Path path) throws IOException {
        return null;//newBufferedReader(path, UTF_8.INSTANCE);
    }

    public static Path setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        log.warn("Files.setAttribute() called but file attribute manipulation is not supported in browser environment. Path: {}, Attribute: {}", path, attribute);
        throw new IOException("Files.setAttribute() is not supported in browser environment. File attribute manipulation cannot be performed.");
    }
}
