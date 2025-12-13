package me.mdbell.awtea.font.tables;

import me.mdbell.awtea.font.ByteReader;

import java.util.HashMap;
import java.util.Map;


public final class KernTable {
	private final Map<Integer, Short> pairKerning = new HashMap<>();

	public KernTable(ByteReader r) {
		// Overall kern header (version 0)
		int version = r.readUInt16();  // should be 0
		int nTables = r.readUInt16();

		for (int i = 0; i < nTables; i++) {
			int subVersion = r.readUInt16();  // usually 0
			int length    = r.readUInt16();
			int coverage  = r.readUInt16();

			int format  = coverage & 0xFF;
			int isHorizontal = (coverage >> 8) & 0x1; // bit 8: horizontal

			int subtableStart = r.position();

			if (format == 0 && isHorizontal == 1) {
				readFormat0Subtable(r);
			} else {
				// Skip unsupported formats
				r.setPosition(subtableStart + (length - 6)); // 6 bytes we already read
			}
		}
	}

	private void readFormat0Subtable(ByteReader r) {
		int nPairs        = r.readUInt16();
		int searchRange   = r.readUInt16();
		int entrySelector = r.readUInt16();
		int rangeShift    = r.readUInt16();

		for (int i = 0; i < nPairs; i++) {
			int left  = r.readUInt16();
			int right = r.readUInt16();
			short value = r.readInt16();   // signed kerning value in font units

			int key = (left << 16) | right;
			// last one wins if duplicates
			pairKerning.put(key, value);
		}
	}

	public int getKerning(int leftGid, int rightGid) {
		int key = (leftGid << 16) | rightGid;
		Short v = pairKerning.get(key);
		return v != null ? v : 0;
	}
}

