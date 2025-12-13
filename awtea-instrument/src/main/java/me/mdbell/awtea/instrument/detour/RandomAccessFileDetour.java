package me.mdbell.awtea.instrument.detour;

import me.mdbell.awtea.instrument.DetourMethod;
import me.mdbell.awtea.instrument.DetourReceiver;
import me.mdbell.awtea.instrument.NoDetours;

import java.io.RandomAccessFile;

@NoDetours
@DetourReceiver(target = RandomAccessFile.class)
public class RandomAccessFileDetour {

	@DetourMethod("<init>")
	public static RandomAccessFile open(java.io.File file, String mode) throws Exception {
		if (mode.contains("w")) {
			// Ensure the file exists when opened in write mode
			if (!file.exists() && !file.createNewFile()) {
				System.err.println("Failed to create file: " + file.getAbsolutePath());
				throw new RuntimeException("Failed to create file: " + file.getAbsolutePath());
			}
		}

		return new RandomAccessFile(file, mode);
	}
}
