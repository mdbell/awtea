package me.mdbell.awtea.font.cmap;

import me.mdbell.awtea.font.ByteReader;

public final class Format13Cmap implements Cmap {
	private final int length;
	private final int nGroups;
	private final int[] startChar;
	private final int[] endChar;
	private final int[] glyphId;

	public Format13Cmap(ByteReader r) {
		int format = r.readUInt16();
		if (format != 13) {
			throw new IllegalArgumentException("Format13Cmap requires format 13, got " + format);
		}

		int reserved = r.readUInt16();        // should be 0
		this.length = (int) r.readUInt32();   // length of this subtable
		int language = (int) r.readUInt32();  // can be ignored

		this.nGroups = (int) r.readUInt32();

		this.startChar = new int[nGroups];
		this.endChar = new int[nGroups];
		this.glyphId = new int[nGroups];

		for (int i = 0; i < nGroups; i++) {
			startChar[i] = (int) r.readUInt32();
			endChar[i]   = (int) r.readUInt32();
			glyphId[i]   = (int) r.readUInt32();
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
				// in range: many-to-one mapping
				return glyphId[mid];
			}
		}

		return 0;
	}

	@Override
	public String toString() {
		return "Format13Cmap{nGroups=" + nGroups + "}";
	}
}
