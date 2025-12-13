package me.mdbell.awtea.font.tables;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.mdbell.awtea.font.ByteReader;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public final class HmtxTable {

	/** Advance widths (uint16 values in font units). */
	private final int[] advanceWidth;

	/** Left side bearings (int16 values in font units). */
	private final short[] leftSideBearing;

	public static HmtxTable read(ByteReader r, HheaTable hhea, MaxpTable maxp) {

		int numGlyphs = maxp.getNumGlyphs();
		int nMetrics  = hhea.getNumberOfHMetrics();

		if (nMetrics <= 0 || nMetrics > numGlyphs) {
			throw new IllegalArgumentException(
				"hhea.numberOfHMetrics (" + nMetrics +
					") must be in range [1, numGlyphs=" + numGlyphs + "]"
			);
		}

		int[] aw  = new int[numGlyphs];
		short[] lsb = new short[numGlyphs];

		int i = 0;

		// ---- Read long hMetrics ----
		for (; i < nMetrics; i++) {
			aw[i]  = r.readUInt16();
			lsb[i] = r.readInt16();
		}

		// ---- Remaining glyphs reuse last advanceWidth but have their own LSB ----
		int lastAdvance = aw[nMetrics - 1];
		for (; i < numGlyphs; i++) {
			aw[i]  = lastAdvance;
			lsb[i] = r.readInt16();
		}

		return HmtxTable.builder()
			.advanceWidth(aw)
			.leftSideBearing(lsb)
			.build();
	}

	public int getAdvanceWidth(int glyphId) {
		if (glyphId < 0 || glyphId >= advanceWidth.length) {
			return 0;
		}
		return advanceWidth[glyphId];
	}

	public short getLeftSideBearing(int glyphId) {
		if (glyphId < 0 || glyphId >= leftSideBearing.length) {
			return 0;
		}
		return leftSideBearing[glyphId];
	}
}
