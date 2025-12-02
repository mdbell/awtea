package me.mdbell.awtea.font.tables;

import lombok.*;
import me.mdbell.awtea.font.ByteReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class NameTable {

	private final int format;

	private final List<NameRecord> records;

	public static NameTable read(ByteReader r) {
		NameTableBuilder builder = builder();


		int format = r.readUInt16();
		int count = r.readUInt16();
		int stringOffset = r.readUInt16();

		builder.format(format);

		// First pass: read raw records (metadata only)
		List<RawRecord> rawRecords = new ArrayList<>(count);
		int maxEnd = 0;

		for (int i = 0; i < count; i++) {
			int platformId = r.readUInt16();
			int encodingId = r.readUInt16();
			int languageId = r.readUInt16();
			int nameId = r.readUInt16();
			int length = r.readUInt16();
			int offset = r.readUInt16();

			RawRecord raw = new RawRecord(
				platformId,
				encodingId,
				languageId,
				nameId,
				length,
				offset
			);
			rawRecords.add(raw);

			int end = offset + length;
			if (end > maxEnd) {
				maxEnd = end;
			}
		}

		// Second pass: build decoded NameRecords
		List<NameRecord> records = new ArrayList<>(count);
		for (RawRecord raw : rawRecords) {
			String text = decodeNameString(r, stringOffset, raw);

			NameRecord rec = NameRecord.builder()
				.platformId(raw.platformId)
				.encodingId(raw.encodingId)
				.languageId(raw.languageId)
				.nameId(raw.nameId)
				.length(raw.length)
				.offset(raw.offset)
				.value(text)
				.build();

			records.add(rec);
		}

		builder.records(Collections.unmodifiableList(records));

		return builder.build();
	}

	public String getFirstNameById(NameIdentifier nameId) {
		for (NameRecord rec : records) {
			if (rec.getNameId() == nameId.getValue()) {
				String v = rec.getValue();
				if (v != null && !v.isEmpty()) {
					return v;
				}
			}
		}
		return null;
	}

	private static String decodeNameString(ByteReader r,
										   int stringOffset,
										   RawRecord raw) {
		if (raw.length <= 0) {
			return null;
		}

		int savePos = r.position(); // relative to table start
		try {
			int strPos = stringOffset + raw.offset;  // still relative to table start
			r.setPosition(strPos);
			switch(PlatformID.fromValue(raw.platformId)) {
				case UNICODE: // Unicode
				case WINDOWS: // Windows
					return r.readUtf16BEString(raw.length);
				case MAC: // Macintosh
				default:
					return r.readAsciiString(raw.length);
			}
		} catch (IllegalArgumentException | IndexOutOfBoundsException e) {
			return null;
		} finally {
			r.setPosition(savePos);
		}
	}

	public String getFontFamilyName() {
		return getFirstNameById(NameIdentifier.FONT_FAMILY);
	}

	public String getFullFontName() {
		return getFirstNameById(NameIdentifier.FULL_FONT_NAME);
	}

	@RequiredArgsConstructor
	private static class RawRecord {
		final int platformId;
		final int encodingId;
		final int languageId;
		final int nameId;
		final int length;
		final int offset;
	}

	@Data
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	@Builder(access = AccessLevel.PRIVATE)
	public static class NameRecord {
		private final int platformId;   // uint16
		private final int encodingId;   // uint16
		private final int languageId;   // uint16
		private final int nameId;       // uint16
		private final int length;       // uint16
		private final int offset;       // uint16, from start of string storage
		private final String value;     // decoded text, or null if decode failed
	}

	@Getter
	public enum PlatformID {
		UNICODE(0),
		MAC(1),
		ISO(2),
		WINDOWS(3);

		private final int value;

		PlatformID(int value) {
			this.value = value;
		}

		public static PlatformID fromValue(int value) {
			for (PlatformID pid : values()) {
				if (pid.value == value) {
					return pid;
				}
			}
			return null;
		}
	}

	@Getter
	public enum NameIdentifier {
		COPYRIGHT_NOTICE(0),
		FONT_FAMILY(1),
		FONT_SUBFAMILY(2),
		FULL_FONT_NAME(4),
		NAME_TABLE_VERSION(5),
		POSTSCRIPT_NAME(6),
		TRADEMARK(7),
		MANUFACTURER_NAME(8),
		DESIGNER(9),
		DESCRIPTION(10),
		VENDOR_URL(11),
		DESIGNER_URL(12),
		LICENSE_DESCRIPTION(13),
		LICENSE_INFO_URL(14),
		RESERVED_15(15),
		PREFERRED_FAMILY(16),
		PREFERRED_SUBFAMILY(17),
		COMPATIBLE_FULL_FONT_NAME(18),
		SAMPLE_TEXT(19),
		// skipping opentype specific ids for now
		VARIATIONS_POSTSCRIPT_NAME_PREFIX(25);

		private final int value;

		NameIdentifier(int value) {
			this.value = value;
		}

		public static NameIdentifier fromValue(int value) {
			for (NameIdentifier nid : values()) {
				if (nid.value == value) {
					return nid;
				}
			}
			return null;
		}
	}
}
