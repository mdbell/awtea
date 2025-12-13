package me.mdbell.awtea.font.cmap;

import me.mdbell.awtea.font.ByteReader;

/**
 * cmap format 14: Unicode Variation Sequences.
 *
 * This does NOT implement Cmap directly, because it operates on
 * (baseCodePoint, variationSelector) pairs, not single code points.
 */
public final class Format14Cmap {
	private static final class VarSelectorRecord {
		final int varSelector;          // the variation selector code point
		final int[] unicodeValues;      // base codepoints with non-default glyphs
		final int[] glyphIds;           // glyph overrides, same length as unicodeValues

		VarSelectorRecord(int varSelector, int[] unicodeValues, int[] glyphIds) {
			this.varSelector = varSelector;
			this.unicodeValues = unicodeValues;
			this.glyphIds = glyphIds;
		}
	}

	// Sorted by varSelector (spec guarantees records are sorted)
	private final VarSelectorRecord[] records;

	public Format14Cmap(ByteReader r) {
		int format = r.readUInt16();
		if (format != 14) {
			throw new IllegalArgumentException("Format14Cmap requires format 14, got " + format);
		}

		// subtable base is position 0 for this reader
		long length = r.readUInt32();
		long numVarSelectorRecords = r.readUInt32();

		if (numVarSelectorRecords > Integer.MAX_VALUE) {
			throw new IllegalStateException("Too many VarSelectorRecords: " + numVarSelectorRecords);
		}

		this.records = new VarSelectorRecord[(int) numVarSelectorRecords];

		for (int i = 0; i < numVarSelectorRecords; i++) {
			int varSelector = readUInt24(r);
			int defaultUVSOffset = (int) r.readUInt32();
			int nonDefaultUVSOffset = (int) r.readUInt32();

			int[] unicodeValues = new int[0];
			int[] glyphIds = new int[0];

			// We completely ignore defaultUVSOffset for now since it does not
			// change the glyph; it only signals that a variation is "supported".
			// Visually, ignoring it is fine: we just use the base glyph.

			if (nonDefaultUVSOffset != 0) {
				ByteReader nonDef = r.forkRelative(nonDefaultUVSOffset);
				int numUVSMappings = (int) nonDef.readUInt32();
				unicodeValues = new int[numUVSMappings];
				glyphIds = new int[numUVSMappings];

				for (int j = 0; j < numUVSMappings; j++) {
					int unicodeValue = readUInt24(nonDef);
					int glyphId = nonDef.readUInt16();
					unicodeValues[j] = unicodeValue;
					glyphIds[j] = glyphId;
				}
			}

			records[i] = new VarSelectorRecord(varSelector, unicodeValues, glyphIds);
		}
	}

	/**
	 * Map (base codepoint, variation selector) to a glyph ID.
	 *
	 * @param baseCp          the base code point (e.g., U+2764)
	 * @param variationSelector the variation selector (e.g., U+FE0F)
	 * @param defaultGlyphId  glyph ID from the base cmap (format 4/12/etc)
	 * @return the overridden glyph if present, otherwise defaultGlyphId
	 */
	public int mapVariant(int baseCp, int variationSelector, int defaultGlyphId) {
		VarSelectorRecord rec = findVarSelectorRecord(variationSelector);
		if (rec == null || rec.unicodeValues.length == 0) {
			return defaultGlyphId;
		}

		int idx = binarySearch(rec.unicodeValues, baseCp);
		if (idx < 0) {
			return defaultGlyphId;
		}

		int gid = rec.glyphIds[idx];
		if (gid == 0) {
			// 0 means "missing glyph" – fall back to default
			return defaultGlyphId;
		}
		return gid;
	}

	private VarSelectorRecord findVarSelectorRecord(int variationSelector) {
		int low = 0;
		int high = records.length - 1;
		while (low <= high) {
			int mid = (low + high) >>> 1;
			int v = records[mid].varSelector;
			if (variationSelector < v) {
				high = mid - 1;
			} else if (variationSelector > v) {
				low = mid + 1;
			} else {
				return records[mid];
			}
		}
		return null;
	}

	private static int binarySearch(int[] arr, int value) {
		int low = 0;
		int high = arr.length - 1;
		while (low <= high) {
			int mid = (low + high) >>> 1;
			int v = arr[mid];
			if (value < v) {
				high = mid - 1;
			} else if (value > v) {
				low = mid + 1;
			} else {
				return mid;
			}
		}
		return -1;
	}

	private static int readUInt24(ByteReader r) {
		int b1 = r.readInt8() & 0xFF;
		int b2 = r.readInt8() & 0xFF;
		int b3 = r.readInt8() & 0xFF;
		return (b1 << 16) | (b2 << 8) | b3;
	}

	@Override
	public String toString() {
		return "Format14Cmap{records=" + records.length + "}";
	}
}
