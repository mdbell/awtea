package me.mdbell.awtea.instrument.detour;

import me.mdbell.awtea.instrument.DetourMethod;
import me.mdbell.awtea.instrument.DetourReceiver;
import me.mdbell.awtea.instrument.NoDetours;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.io.File;

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
	public static Object toPath(File file) {
		log.warn("File.toPath() called but java.nio.file.Path is not supported in browser environment. File: {}", file.getAbsolutePath());
		throw new UnsupportedOperationException("File.toPath() is not supported in browser environment. Use File APIs directly instead.");
	}
}
