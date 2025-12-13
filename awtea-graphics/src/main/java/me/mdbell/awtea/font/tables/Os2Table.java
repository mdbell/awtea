package me.mdbell.awtea.font.tables;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.mdbell.awtea.font.ByteReader;

/**
 * OS/2 and Windows Metrics table.
 *
 * Supports versions 0–5 as per the OpenType spec.
 */
@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public final class Os2Table {

	private final int version;


	private final short xAvgCharWidth;
	private final int   usWeightClass;
	private final int   usWidthClass;
	private final int   fsType;

	private final short ySubscriptXSize;
	private final short ySubscriptYSize;
	private final short ySubscriptXOffset;
	private final short ySubscriptYOffset;

	private final short ySuperscriptXSize;
	private final short ySuperscriptYSize;
	private final short ySuperscriptXOffset;
	private final short ySuperscriptYOffset;

	private final short yStrikeoutSize;
	private final short yStrikeoutPosition;

	private final short sFamilyClass;

	private final byte[] panose;

	// --- Unicode & codepage ranges ---

	private final long ulUnicodeRange1;    // UINT32
	private final long ulUnicodeRange2;    // UINT32
	private final long ulUnicodeRange3;    // UINT32
	private final long ulUnicodeRange4;    // UINT32

	private final String achVendID;        // 4-char vendor ID

	private final int fsSelection;         // UINT16 (selection bits)
	private final int usFirstCharIndex;    // UINT16
	private final int usLastCharIndex;     // UINT16

	// Typographic metrics

	private final short sTypoAscender;
	private final short sTypoDescender;
	private final short sTypoLineGap;

	// Windows metrics

	private final int usWinAscent;         // UINT16
	private final int usWinDescent;        // UINT16

	// v1+ codepage ranges

	private final long ulCodePageRange1;   // UINT32
	private final long ulCodePageRange2;   // UINT32

	// v2+ extra metrics

	private final short sxHeight;
	private final short sCapHeight;
	private final int   usDefaultChar;     // UINT16
	private final int   usBreakChar;       // UINT16
	private final int   usMaxContext;      // UINT16

	// v5+ optical sizes

	private final int   usLowerOpticalPointSize; // UINT16
	private final int   usUpperOpticalPointSize; // UINT16

	public static Os2Table read(ByteReader r) {
		Os2TableBuilder b = builder();

		int version = r.readUInt16();
		b.version(version);

		if (version < 0 || version > 5) {
			// You can relax this if you want to be future-proof
			// and just parse as v5 and ignore trailing bytes.
			throw new IllegalArgumentException("Unsupported OS/2 version: " + version);
		}

		// --- Common fields for all versions (0+) ---

		b.xAvgCharWidth(r.readInt16());
		b.usWeightClass(r.readUInt16());
		b.usWidthClass(r.readUInt16());
		b.fsType(r.readInt16());

		b.ySubscriptXSize(r.readInt16());
		b.ySubscriptYSize(r.readInt16());
		b.ySubscriptXOffset(r.readInt16());
		b.ySubscriptYOffset(r.readInt16());

		b.ySuperscriptXSize(r.readInt16());
		b.ySuperscriptYSize(r.readInt16());
		b.ySuperscriptXOffset(r.readInt16());
		b.ySuperscriptYOffset(r.readInt16());

		b.yStrikeoutSize(r.readInt16());
		b.yStrikeoutPosition(r.readInt16());

		b.sFamilyClass(r.readInt16());

		byte[] panose = new byte[10];
		for (int i = 0; i < 10; i++) {
			panose[i] = (byte) r.readUInt8();
		}
		b.panose(panose);

		b.ulUnicodeRange1(r.readUInt32());
		b.ulUnicodeRange2(r.readUInt32());
		b.ulUnicodeRange3(r.readUInt32());
		b.ulUnicodeRange4(r.readUInt32());

		// achVendID: 4 bytes, usually ASCII
		char[] vend = new char[4];
		for (int i = 0; i < 4; i++) {
			vend[i] = (char) (r.readUInt8() & 0xFF);
		}
		b.achVendID(new String(vend));

		b.fsSelection(r.readUInt16());
		b.usFirstCharIndex(r.readUInt16());
		b.usLastCharIndex(r.readUInt16());

		b.sTypoAscender(r.readInt16());
		b.sTypoDescender(r.readInt16());
		b.sTypoLineGap(r.readInt16());

		b.usWinAscent(r.readUInt16());
		b.usWinDescent(r.readUInt16());

		// --- v1+ code page ranges ---

		long codePageRange1 = 0;
		long codePageRange2 = 0;
		if (version >= 1) {
			codePageRange1 = r.readUInt32();
			codePageRange2 = r.readUInt32();
		}
		b.ulCodePageRange1(codePageRange1);
		b.ulCodePageRange2(codePageRange2);

		// --- v2+ extra metrics ---

		short sxHeight = 0;
		short sCapHeight = 0;
		int usDefaultChar = 0;
		int usBreakChar = 0;
		int usMaxContext = 0;

		if (version >= 2) {
			sxHeight = r.readInt16();
			sCapHeight = r.readInt16();
			usDefaultChar = r.readUInt16();
			usBreakChar = r.readUInt16();
			usMaxContext = r.readUInt16();
		}

		b.sxHeight(sxHeight);
		b.sCapHeight(sCapHeight);
		b.usDefaultChar(usDefaultChar);
		b.usBreakChar(usBreakChar);
		b.usMaxContext(usMaxContext);

		// --- v5+ optical sizes ---

		int usLowerOpticalPointSize = 0;
		int usUpperOpticalPointSize = 0;

		if (version >= 5) {
			usLowerOpticalPointSize = r.readUInt16();
			usUpperOpticalPointSize = r.readUInt16();
		}

		b.usLowerOpticalPointSize(usLowerOpticalPointSize);
		b.usUpperOpticalPointSize(usUpperOpticalPointSize);

		return b.build();
	}

	// ---------------- Convenience helpers ----------------

	/** Typographic line height in font units: ascender - descender + lineGap. */
	public int getTypoLineHeight() {
		return (int) sTypoAscender - (int) sTypoDescender + (int) sTypoLineGap;
	}

	/** True if the font wants layout engines to use OS/2 typo metrics. */
	public boolean useTypoMetricsForLineSpacing() {
		// fsSelection bit 7 (0x80) = USE_TYPO_METRICS
		return (fsSelection & 0x0080) != 0;
	}

	public boolean isItalic() {
		// fsSelection bit 0
		return (fsSelection & 0x0001) != 0;
	}

	public boolean isBold() {
		// fsSelection bit 5
		return (fsSelection & 0x0020) != 0;
	}

	public boolean isRegular() {
		// fsSelection bit 6
		return (fsSelection & 0x0040) != 0;
	}
}
