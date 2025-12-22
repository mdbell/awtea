package me.mdbell.awtea.instrument.detour;

import me.mdbell.awtea.instrument.DetourMethod;
import me.mdbell.awtea.instrument.DetourReceiver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@DetourReceiver(target = Files.class)
public class FilesDetour {

    @DetourMethod
    public static Path setAttribute(Path path, String attribute, Object value) throws IOException {
        throw new IOException("Files.setAttribute() is not supported in browser environment. File attribute manipulation cannot be performed.");
    }
}
