package me.mdbell.awtea.instrument.detour;

import me.mdbell.awtea.instrument.DetourMethod;
import me.mdbell.awtea.instrument.DetourReceiver;
import me.mdbell.awtea.instrument.NoDetours;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Detour for java.io.File methods that are not supported in browser environments.
 * Provides stubs that log warnings when unsupported methods are called.
 */
@NoDetours
@DetourReceiver(target = File.class)
public class FileDetour {

	private static final Logger log = LoggerFactory.getLogger(FileDetour.class);

	/**
	 * Detour for File.toPath() - not supported in browser.
	 * Returns null and logs a warning.
	 */
	@DetourMethod("toPath")
	public static Path toPath(File file) throws IOException {
		return Path.of(file.getAbsolutePath());
	}
}
