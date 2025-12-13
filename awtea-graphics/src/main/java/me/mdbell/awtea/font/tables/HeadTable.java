package me.mdbell.awtea.font.tables;

import lombok.*;
import me.mdbell.awtea.font.ByteReader;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class HeadTable {

	private final int version;
	private final int fontRevision;

	private final long checksumAdjustment;

	private final int flags;

	private final long created;
	private final long modified;

	private final int unitsPerEm;
	private final short xMin;
	private final short yMin;
	private final short xMax;
	private final short yMax;

	private final int macStyle;
	private final int lowestRecPPEM;
	private final int fontDirectionHint;
	private final int indexToLocFormat;
	private final short glyphDataFormat;

	public static HeadTable read(ByteReader r) {
		HeadTable.HeadTableBuilder builder = builder();

		// version and fontRevision are 16.16 fixed; we’ll just keep raw 32-bit ints
		builder.version(r.readInt32())
			.fontRevision(r.readInt32())
			.checksumAdjustment(r.readUInt32());

		long magicNumber = r.readUInt32();

		if (magicNumber != 0x5F0F3CF5L) {
			throw new IllegalArgumentException("head.magicNumber invalid: 0x" +
				Long.toHexString(magicNumber));
		}

		builder.flags(r.readUInt16())
			.unitsPerEm(r.readUInt16())
			.created(r.readInt64())
			.modified(r.readInt64())
			.xMin(r.readInt16())
			.yMin(r.readInt16())
			.xMax(r.readInt16())
			.yMax(r.readInt16())
			.macStyle(r.readUInt16())
			.lowestRecPPEM(r.readUInt16())
			.fontDirectionHint(r.readInt16())
			.indexToLocFormat(r.readInt16())
			.glyphDataFormat(r.readInt16());

		if (builder.glyphDataFormat != 0) {
			throw new IllegalArgumentException("Unsupported glyphDataFormat: " + builder.glyphDataFormat);
		}

		// indexToLocFormat must be 0 (short offsets) or 1 (long offsets)
		if (builder.indexToLocFormat != 0 && builder.indexToLocFormat != 1) {
			throw new IllegalArgumentException("Unsupported indexToLocFormat: " + builder.indexToLocFormat);
		}

		return builder.build();
	}

	public float getVersionFloat() {
		return version / 65536.0f;
	}

	public float getFontRevisionFloat() {
		return fontRevision / 65536.0f;
	}

	public boolean isBaselineAtYZero() {
		// bit 1 of flags usually means "baseline at y = 0"
		return (flags & 0x0002) != 0;
	}

	public boolean isItalic() {
		return (macStyle & 0x0002) != 0;
	}

	public boolean isBold() {
		return (macStyle & 0x0001) != 0;
	}
}
