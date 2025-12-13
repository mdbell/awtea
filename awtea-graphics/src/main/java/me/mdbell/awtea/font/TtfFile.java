package me.mdbell.awtea.font;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TtfFile {

	private final byte[] data;
	@Getter
	private final int scalerType;
	@Getter
	private final int numTables;

	@Getter
	private final Map<String, TtfTableRecord> tables;

	public TtfFile(byte[] data) {
		this.data = data;
		ByteReader r = new ByteReader(data);

		// SFNT header
		this.scalerType = r.readInt32();     // e.g. 0x00010000 or 'OTTO'
		this.numTables  = r.readUInt16();
		int searchRange = r.readUInt16();
		int entrySelector = r.readUInt16();
		int rangeShift = r.readUInt16();

		Map<String, TtfTableRecord> t = new HashMap<>();
		for (int i = 0; i < numTables; i++) {
			String tag = r.readTag();
			long checksum = r.readUInt32();
			int offset = r.readInt32();
			int length = r.readInt32();
			t.put(tag, new TtfTableRecord(tag, checksum, offset, length));
		}
		this.tables = Collections.unmodifiableMap(t);
	}

	public TtfTableRecord getTable(String tag) {
		return tables.get(tag);
	}

	public ByteReader slice(String tag) {
		TtfTableRecord tr = tables.get(tag);
		if (tr == null) return null;
		return new ByteReader(data, tr.offset, tr.length);
	}

}
