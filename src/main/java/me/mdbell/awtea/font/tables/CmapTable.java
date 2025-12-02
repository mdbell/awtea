package me.mdbell.awtea.font.tables;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.mdbell.awtea.font.cmap.*;
import org.jetbrains.annotations.NotNull;
import me.mdbell.awtea.font.ByteReader;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class CmapTable {

	/**
	 * Raw cmap table version (usually 0).
	 */
	private final int version;

	/**
	 * Number of encoding records.
	 */
	private final int numTables;

	/**
	 * The primary cmap subtable chosen by our preference rules.
	 */
	private final Cmap cmap;

	/**
	 * Optional format 14 cmap (Unicode Variation Sequences).
	 * May be {@code null} if not present in the font.
	 */
	private final Format14Cmap cmap14;

	/**
	 * The encoding record we ended up using for {@link #cmap}.
	 * Useful for debugging/logging.
	 */
	private final EncRecord chosenRecord;

	public static CmapTable read(ByteReader r) {
		int version = r.readUInt16();
		int numTables = r.readUInt16();

		List<EncRecord> records = new ArrayList<>(numTables);
		int vsRecordIdx = -1;

		// First pass: read encoding records and peek format
		for (int i = 0; i < numTables; i++) {
			int platformId = r.readUInt16();
			int encodingId = r.readUInt16();
			int offset = r.readInt32();  // from start of cmap table

			ByteReader sub = r.forkRelative(offset);
			int format = sub.readUInt16();  // peek

			EncRecord rec = EncRecord.builder()
				.platformId(platformId)
				.encodingId(encodingId)
				.offset(offset)
				.format(format)
				.build();

			records.add(rec);

			// Capture format 14 (Unicode variation sequences: platform 0, encoding 5)
			if (rec.is(0, 5, 14)) {
				vsRecordIdx = i;
			}
		}

		// Pick the best encoding record (preference order)
		EncRecord chosen = getEncRecord(records);

		// Parse primary cmap subtable
		ByteReader sub = r.forkRelative(chosen.getOffset());
		sub.setPosition(0); // let the subtable parser re-read from 0

		int format = chosen.getFormat();
		Cmap primaryCmap;

		switch (format) {
			case 0:
				primaryCmap = new Format0Cmap(sub);
				break;
			case 4:
				primaryCmap = new Format4Cmap(sub);
				break;
			case 12:
				primaryCmap = new Format12Cmap(sub);
				break;
			case 13:
				primaryCmap = new Format13Cmap(sub);
				break;
			default:
				throw new UnsupportedOperationException(
					"cmap: chosen subtable has unsupported format " + format + " (" + chosen + ")"
				);
		}

		// Optional format 14 subtable
		Format14Cmap variationCmap = null;
		if (vsRecordIdx != -1) {
			EncRecord vsRecord = records.get(vsRecordIdx);
			ByteReader vsSub = r.forkRelative(vsRecord.getOffset());
			vsSub.setPosition(0);
			variationCmap = new Format14Cmap(vsSub);
		}

		return CmapTable.builder()
			.version(version)
			.numTables(numTables)
			.cmap(primaryCmap)
			.cmap14(variationCmap)
			.chosenRecord(chosen)
			.build();
	}

	@NotNull
	private static EncRecord getEncRecord(List<EncRecord> records) {
		EncRecord chosen;

		// 1) Prefer format 12 Unicode full: (3,10) or (0,4)
		chosen = pick(records, 3, 10, 12);
		if (chosen == null) {
			chosen = pick(records, 0, 4, 12);
		}

		// 2) Then format 4 Windows BMP: (3,1)
		if (chosen == null) {
			chosen = pick(records, 3, 1, 4);
		}

		// 3) Any other format 12
		if (chosen == null) {
			chosen = pickAnyFormat(records, 12);
		}

		// 4) Any other format 4
		if (chosen == null) {
			chosen = pickAnyFormat(records, 4);
		}

		// 5) Any format 13 (many-to-one mapping)
		if (chosen == null) {
			chosen = pickAnyFormat(records, 13);
		}

		// 6) Any format 0 (very basic fallback)
		if (chosen == null) {
			chosen = pickAnyFormat(records, 0);
		}

		if (chosen == null) {
			throw new IllegalStateException("cmap: no usable subtable found");
		}
		return chosen;
	}

	private static EncRecord pick(List<EncRecord> records, int platform, int encoding, int format) {
		for (EncRecord rec : records) {
			if (rec.is(platform, encoding, format)) {
				return rec;
			}
		}
		return null;
	}

	private static EncRecord pickAnyFormat(List<EncRecord> records, int format) {
		for (EncRecord rec : records) {
			if(rec.isFormat(format)) {
				return rec;
			}
		}
		return null;
	}
}
