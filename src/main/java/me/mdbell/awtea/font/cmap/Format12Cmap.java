package me.mdbell.awtea.font.cmap;

import me.mdbell.awtea.font.ByteReader;

public final class Format12Cmap implements Cmap {
	private final int length;
	private final int nGroups;
	private final int[] startChar;
	private final int[] endChar;
	private final int[] startGlyph;

	public Format12Cmap(ByteReader r) {
		int format = r.readUInt16();
		if (format != 12) {
			throw new IllegalArgumentException("Format12Cmap requires format 12, got " + format);
		}

		int reserved = r.readUInt16();   // should be 0
		this.length = (int) r.readUInt32();  // length of this subtable
		int language = (int) r.readUInt32();

		this.nGroups = (int) r.readUInt32();

		this.startChar = new int[nGroups];
		this.endChar = new int[nGroups];
		this.startGlyph = new int[nGroups];

		for (int i = 0; i < nGroups; i++) {
			startChar[i] = (int) r.readUInt32();
			endChar[i] = (int) r.readUInt32();
			startGlyph[i] = (int) r.readUInt32();
		}
	}

	@Override
	public int mapCodePointToGlyph(int cp) {
		if (cp < 0) return 0;

		int low = 0;
		int high = nGroups - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			int start = startChar[mid];
			int end = endChar[mid];

			if (cp < start) {
				high = mid - 1;
			} else if (cp > end) {
				low = mid + 1;
			} else {
				// in range
				return startGlyph[mid] + (cp - start);
			}
		}

		return 0;
	}

	@Override
	public String toString() {
		return "Format12Cmap{nGroups=" + nGroups + "}";
	}
}

