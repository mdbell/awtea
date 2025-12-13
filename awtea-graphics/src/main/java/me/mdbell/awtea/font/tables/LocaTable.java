package me.mdbell.awtea.font.tables;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.mdbell.awtea.font.ByteReader;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public final class LocaTable {

	/**
	 * Offsets into the 'glyf' table, length = numGlyphs + 1.
	 * Values are in bytes from the start of the glyf table.
	 */
	private final int[] offsets;

	public static LocaTable read(ByteReader r, HeadTable head, MaxpTable maxp) {
		int numGlyphs = maxp.getNumGlyphs();
		int[] offsets = new int[numGlyphs + 1];

		int indexToLocFormat = head.getIndexToLocFormat();

		switch (indexToLocFormat) {
			case 0:
				// Short format: uint16, stored as offset/2
				for (int i = 0; i <= numGlyphs; i++) {
					int v = r.readUInt16();
					offsets[i] = v * 2;
				}
				break;

			case 1:
				// Long format: uint32 actual byte offsets
				for (int i = 0; i <= numGlyphs; i++) {
					long v = r.readUInt32();
					// Most fonts are < 2GB, so int cast is fine
					offsets[i] = (int) v;
				}
				break;

			default:
				throw new IllegalStateException(
					"Unsupported indexToLocFormat: " + indexToLocFormat
				);
		}

		// Basic monotonicity sanity check (not strictly required, but nice)
		for (int i = 0; i < numGlyphs; i++) {
			if (offsets[i + 1] < offsets[i]) {
				throw new IllegalArgumentException(
					"loca offsets not monotonic at index " + i +
						": " + offsets[i] + " -> " + offsets[i + 1]
				);
			}
		}

		return LocaTable.builder()
			.offsets(offsets)
			.build();
	}

	public int getGlyphOffset(int glyphId) {
		if (glyphId < 0 || glyphId >= offsets.length - 1) {
			return 0;
		}
		return offsets[glyphId];
	}

	public int getGlyphLength(int glyphId) {
		if (glyphId < 0 || glyphId >= offsets.length - 1) {
			return 0;
		}
		return offsets[glyphId + 1] - offsets[glyphId];
	}
}
