package me.mdbell.awtea.font.tables;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.mdbell.awtea.font.ByteReader;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public final class HheaTable {

	// 16.16 fixed
	private final int version;

	// Core vertical metrics (font units)
	private final short ascender;
	private final short descender;
	private final short lineGap;

	// Horizontal metrics / layout hints
	private final int advanceWidthMax;        // uint16
	private final short minLeftSideBearing;
	private final short minRightSideBearing;
	private final short xMaxExtent;
	private final short caretSlopeRise;
	private final short caretSlopeRun;
	private final short caretOffset;

	// metricDataFormat almost always 0
	private final short metricDataFormat;

	// Number of long hMetric records in hmtx
	private final int numberOfHMetrics;       // uint16

	public static HheaTable read(ByteReader r) {
		HheaTableBuilder b = builder();

		int version = r.readInt32(); // 0x00010000 for TrueType
		b.version(version);

		b.ascender(r.readInt16())
			.descender(r.readInt16())
			.lineGap(r.readInt16())
			.advanceWidthMax(r.readUInt16())
			.minLeftSideBearing(r.readInt16())
			.minRightSideBearing(r.readInt16())
			.xMaxExtent(r.readInt16())
			.caretSlopeRise(r.readInt16())
			.caretSlopeRun(r.readInt16())
			.caretOffset(r.readInt16());

		// reserved[4] (each int16), always 0 per spec
		r.skip(4 * 2);

		b.metricDataFormat(r.readInt16())
			.numberOfHMetrics(r.readUInt16());

		// Basic sanity checks
		if (b.metricDataFormat != 0) {
			throw new IllegalArgumentException(
				"Unsupported hhea.metricDataFormat: " + b.metricDataFormat
			);
		}

		if (b.numberOfHMetrics <= 0) {
			throw new IllegalArgumentException(
				"hhea.numberOfHMetrics must be > 0, was " + b.numberOfHMetrics
			);
		}

		return b.build();
	}

	// Optional convenience helpers if you want them later:

	public float getVersionFloat() {
		return version / 65536.0f;
	}

	public int getLineHeight() {
		// Commonly ascender - descender + lineGap
		return ascender - descender + lineGap;
	}
}
