package me.mdbell.awtea.font.cmap;

import me.mdbell.awtea.font.ByteReader;

public final class Format0Cmap implements Cmap {
	private final int length;
	private final int language;
	private final int[] glyphIdArray = new int[256];

	public Format0Cmap(ByteReader r) {
		int format = r.readUInt16();
		if (format != 0) {
			throw new IllegalArgumentException("Format0Cmap requires format 0, got " + format);
		}

		this.length = r.readUInt16();   // length of this subtable in bytes
		this.language = r.readUInt16(); // often 0, can usually be ignored

		// 256 bytes, unsigned
		for (int i = 0; i < 256; i++) {
			int b = r.readInt8() & 0xFF;   // or r.readUInt8() if you have that
			glyphIdArray[i] = b;
		}
	}

	@Override
	public int mapCodePointToGlyph(int cp) {
		if (cp < 0 || cp > 0xFF) {
			return 0; // outside 0..255 not representable in format 0
		}
		return glyphIdArray[cp];
	}

	@Override
	public String toString() {
		return "Format0Cmap{length=" + length + ", language=" + language + "}";
	}
}
