package me.mdbell.awtea.font;

import lombok.Getter;

@Getter
public final class TtfTableRecord {
	public final String tag;
	public final long checksum;
	public final int offset;
	public final int length;

	public TtfTableRecord(String tag, long checksum, int offset, int length) {
		this.tag = tag;
		this.checksum = checksum;
		this.offset = offset;
		this.length = length;
	}

	@Override
	public String toString() {
		return "TtfTableRecord{" +
			"tag='" + tag + '\'' +
			", checksum=0x" + Long.toHexString(checksum) +
			", offset=" + offset +
			", length=" + length +
			'}';
	}
}
