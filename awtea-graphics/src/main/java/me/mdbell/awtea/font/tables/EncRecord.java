package me.mdbell.awtea.font.tables;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EncRecord {
	int platformId;
	int encodingId;
	int offset;
	int format;

	boolean is(int p, int e, int f) {
		return this.platformId == p && this.encodingId == e && this.format == f;
	}

	boolean isFormat(int f) {
		return this.format == f;
	}
}
