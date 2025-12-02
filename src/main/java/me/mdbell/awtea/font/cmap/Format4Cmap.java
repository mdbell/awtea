package me.mdbell.awtea.font.cmap;

import me.mdbell.awtea.font.ByteReader;

public final class Format4Cmap implements Cmap {
	private final ByteReader reader;   // scoped to this subtable
	private final int length;

	private final int segCount;
	private final int[] endCode;
	private final int[] startCode;
	private final short[] idDelta;
	private final int[] idRangeOffset;

	// Byte offsets (relative to subtable start)
	private final int idRangeOffsetOffset;
	private final int glyphIdArrayOffset;

	public Format4Cmap(ByteReader reader) {
		this.reader = reader;

		// subtable starts at position 0 in this reader

		int format = reader.readUInt16();
		if (format != 4) {
			throw new IllegalArgumentException("Format4Cmap requires format 4, got " + format);
		}

		this.length = reader.readUInt16();
		int language = reader.readUInt16();

		int segCountX2 = reader.readUInt16();
		this.segCount = segCountX2 / 2;

		int searchRange = reader.readUInt16();
		int entrySelector = reader.readUInt16();
		int rangeShift = reader.readUInt16();

		this.endCode = new int[segCount];
		for (int i = 0; i < segCount; i++) {
			endCode[i] = reader.readUInt16();
		}

		int reservedPad = reader.readUInt16(); // should be 0

		this.startCode = new int[segCount];
		for (int i = 0; i < segCount; i++) {
			startCode[i] = reader.readUInt16();
		}

		this.idDelta = new short[segCount];
		for (int i = 0; i < segCount; i++) {
			idDelta[i] = reader.readInt16();
		}

		// Record where idRangeOffset[] starts, relative to subtable start
		this.idRangeOffsetOffset = reader.position();

		this.idRangeOffset = new int[segCount];
		for (int i = 0; i < segCount; i++) {
			idRangeOffset[i] = reader.readUInt16();
		}

		// glyphIdArray begins immediately after the last idRangeOffset
		this.glyphIdArrayOffset = reader.position();
	}

	@Override
	public int mapCodePointToGlyph(int codePoint) {
		if (codePoint < 0 || codePoint > 0xFFFF) {
			return 0; // format 4 only covers BMP
		}

		int cp = codePoint;

		// 1. Find segment: first segment whose endCode >= cp
		int i = findSegment(cp);
		if (i < 0) {
			return 0;
		}

		if (cp < startCode[i] || cp > endCode[i]) {
			return 0;
		}

		int rangeOffset = idRangeOffset[i];

		if (rangeOffset == 0) {
			// Simple case: glyphId = (cp + idDelta[i]) & 0xFFFF
			return (cp + idDelta[i]) & 0xFFFF;
		}

		// Complex case: glyph IDs are in glyphIdArray
		int offsetInSegment = cp - startCode[i];

		int glyphIndexAddr =
			idRangeOffsetOffset + 2 * i   // byte offset of idRangeOffset[i]
				+ rangeOffset                   // jump into glyphIdArray
				+ 2 * offsetInSegment;          // pick glyph for this cp

		// Bounds check against subtable length
		if (glyphIndexAddr < glyphIdArrayOffset || glyphIndexAddr + 2 > length) {
			return 0;
		}

		ByteReader g = reader.forkRelative(glyphIndexAddr);
		int glyphId = g.readUInt16();

		if (glyphId == 0) {
			return 0;
		}

		glyphId = (glyphId + idDelta[i]) & 0xFFFF;
		return glyphId;
	}

	private int findSegment(int cp) {
		int low = 0;
		int high = segCount - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			int end = endCode[mid];

			if (cp > end) {
				low = mid + 1;
			} else {
				high = mid - 1;
			}
		}

		int idx = low;
		if (idx < 0 || idx >= segCount) {
			return -1;
		}
		return idx;
	}

	@Override
	public String toString() {
		return "Format4Cmap{segCount=" + segCount + "}";
	}
}


