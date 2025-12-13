package me.mdbell.awtea.instrument.detour;

import me.mdbell.awtea.instrument.DetourMethod;
import me.mdbell.awtea.instrument.DetourReceiver;
import me.mdbell.awtea.instrument.NoDetours;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.io.RandomAccessFile;

@NoDetours
@DetourReceiver(target = RandomAccessFile.class)
public class RandomAccessFileDetour {

	private static final Logger log = LoggerFactory.getLogger(RandomAccessFileDetour.class);

	@DetourMethod("<init>")
	public static RandomAccessFile open(java.io.File file, String mode) throws Exception {
		if (mode.contains("w")) {
			// Ensure the file exists when opened in write mode
			if (!file.exists() && !file.createNewFile()) {
				log.error("Failed to create file: {}", file.getAbsolutePath());
				throw new RuntimeException("Failed to create file: " + file.getAbsolutePath());
			}
		}

		return new RandomAccessFile(file, mode);
	}
}
