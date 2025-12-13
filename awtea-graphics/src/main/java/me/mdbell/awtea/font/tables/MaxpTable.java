package me.mdbell.awtea.font.tables;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.mdbell.awtea.font.ByteReader;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public final class MaxpTable {
	private final int version;
	private final int numGlyphs;

	private final int maxPoints;
	private final int maxContours;
	private final int maxCompositePoints;
	private final int maxCompositeContours;
	private final int maxZones;
	private final int maxTwilightPoints;
	private final int maxStorage;
	private final int maxFunctionDefs;
	private final int maxInstructionDefs;
	private final int maxStackElements;
	private final int maxSizeOfInstructions;
	private final int maxComponentElements;
	private final int maxComponentDepth;

	public static MaxpTable read(ByteReader r){
		MaxpTableBuilder b = builder();

		int version = r.readInt32();  // 16.16
		b.version(version);

		int major = (version >> 16) & 0xFFFF;

		b.numGlyphs(r.readUInt16());

		if (version == 0x00010000) {
			// Full TrueType maxp
			b.maxPoints(r.readUInt16())
				.maxContours(r.readUInt16())
				.maxCompositePoints(r.readUInt16())
				.maxCompositeContours(r.readUInt16())
				.maxZones(r.readUInt16())
				.maxTwilightPoints(r.readUInt16())
				.maxStorage(r.readUInt16())
				.maxFunctionDefs(r.readUInt16())
				.maxInstructionDefs(r.readUInt16())
				.maxStackElements(r.readUInt16())
				.maxSizeOfInstructions(r.readUInt16())
				.maxComponentElements(r.readUInt16())
				.maxComponentDepth(r.readUInt16());
		} else if (version == 0x00005000) {
			// CFF fonts (OpenType w/ CFF outlines) use a tiny version of maxp.
			// Only numGlyphs is present.
			// The rest should be 0.
		} else {
			throw new IllegalArgumentException("Unknown maxp version: " + Integer.toHexString(version));
		}

		return b.build();
	}

}

