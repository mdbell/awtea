package me.mdbell.awtea.detour;

import java.io.RandomAccessFile;

@NoDetours
public class RandomAccessFileDetour {

	public static RandomAccessFile open(java.io.File file, String mode) throws Exception {
		System.out.println("RandomAccessFileDetour.open called with file: " + file.getAbsolutePath() + ", mode: " + mode);
		if(mode.contains("w")){
			// Ensure the file exists when opened in write mode
			if(!file.exists() && !file.createNewFile()) {
				System.err.println("Failed to create file: " + file.getAbsolutePath());
				throw new RuntimeException("Failed to create file: " + file.getAbsolutePath());
			}
		}

		return new RandomAccessFile(file, mode);
	}
}
