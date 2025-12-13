package me.mdbell.awtea.font;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import lombok.*;
import me.mdbell.awtea.font.cmap.Format14Cmap;
import me.mdbell.awtea.font.tables.*;

@Value
@Builder(access = AccessLevel.PRIVATE)
public class TrueTypeFont {

	private static final Logger log = LoggerFactory.getLogger(TrueTypeFont.class);
	TtfFile file;
	NameTable nameTable;
	HeadTable headTable;
	MaxpTable maxpTable;
	CmapTable cmapTable;
	LocaTable locaTable;
	GlyfTable glyfTable;
	HheaTable hheaTable;
	HmtxTable hmtxTable;
	KernTable kernTable;
	Os2Table os2Table;

	public static TrueTypeFont read(byte[] data) {
		TtfFile ttf = new TtfFile(data);

		TrueTypeFontBuilder builder = builder()
			.file(ttf)
			.nameTable(NameTable.read(loadOrThrow(ttf, "name")))
			.headTable(HeadTable.read(loadOrThrow(ttf, "head")))
			.maxpTable(MaxpTable.read(loadOrThrow(ttf, "maxp")))
			.cmapTable(CmapTable.read(loadOrThrow(ttf, "cmap")))
			.hheaTable(HheaTable.read(loadOrThrow(ttf, "hhea")));

		builder.hmtxTable(HmtxTable.read(loadOrThrow(ttf,"hmtx"), builder.hheaTable, builder.maxpTable))
			.locaTable(LocaTable.read(loadOrThrow(ttf, "loca"), builder.headTable, builder.maxpTable));

		builder.glyfTable(new GlyfTable(loadOrThrow(ttf, "glyf"), builder.locaTable, builder.maxpTable));

		ByteReader kernReader = ttf.slice("kern");

		if(kernReader != null){
			builder.kernTable(new KernTable(kernReader));
		}

		ByteReader os2Reader = ttf.slice("OS/2");

		if(os2Reader != null){
			log.info("OS/2 table found!");
			builder.os2Table(Os2Table.read(os2Reader));
		}

		return builder.build();
	}

	private static ByteReader loadOrThrow(TtfFile ttf, String table){
		ByteReader reader = ttf.slice(table);

		if(reader == null) {
			throw new IllegalStateException("missing " + table);
		}
		return reader;
	}

	public Glyph loadGlyph(int glyphId) {
		return glyfTable.loadGlyph(glyphId);
	}

	public Glyph loadGlyphForCodePoint(int cp) {
		int gid = glyphForCodePoint(cp);
		if (gid == 0) {
			return null;
		}
		return glyfTable.loadGlyph(gid);
	}

	public int glyphForCodePoint(int codePoint) {
		return cmapTable.getCmap().mapCodePointToGlyph(codePoint);
	}

	public int glyphForCodePointWithVariation(int codePoint, int variationSelector) {
		int baseGlyph = glyphForCodePoint(codePoint);
		Format14Cmap cmap14 = cmapTable.getCmap14();
		if (cmap14 == null || baseGlyph == 0) {
			return baseGlyph;
		}
		return cmap14.mapVariant(codePoint, variationSelector, baseGlyph);
	}

	// metadata

	public String getFamily() {
		return nameTable.getFirstNameById(NameTable.NameIdentifier.FONT_FAMILY);
	}

	public String getFullName() {
		return nameTable.getFirstNameById(NameTable.NameIdentifier.FULL_FONT_NAME);
	}

	public String getPostScriptName() {
		return nameTable.getFirstNameById(NameTable.NameIdentifier.POSTSCRIPT_NAME);
	}

	public boolean isBold() {
		if(os2Table != null){
			return os2Table.isBold();
		}
		return headTable.isBold();
	}

	public boolean isItalic() {
		if(os2Table != null){
			return os2Table.isItalic();
		}
		return headTable.isItalic();
	}

	// font metrics

	public int getAdvanceWidthUnits(int glyphId) {
		return hmtxTable.getAdvanceWidth(glyphId);
	}

	public short getLeftSideBearingUnits(int glyphId) {
		return hmtxTable.getLeftSideBearing(glyphId);
	}

	public short getAscentUnits()  {
		if(os2Table != null){
			return os2Table.getSTypoAscender();
		}
		return hheaTable.getAscender();
	}
	public short getDescentUnits() {
		if(os2Table != null){
			return os2Table.getSTypoDescender();
		}
		return hheaTable.getDescender();
	}
	public short getLineGapUnits() {
		if(os2Table != null) {
			return os2Table.getSTypoLineGap();
		}
		return hheaTable.getLineGap();
	}

	public int getUnitsPerEm() {
		return headTable.getUnitsPerEm();
	}

	public float getAdvanceWidthPx(int glyphId, float pixelSize) {
		float s = getScaleForPixelSize(pixelSize);
		return getAdvanceWidthUnits(glyphId) * s;
	}

	public float getAscentPx(float pixelSize) {
		float s = getScaleForPixelSize(pixelSize);
		return getAscentUnits() * s;
	}

	public float getDescentPx(float pixelSize) {
		float s = getScaleForPixelSize(pixelSize);
		return getDescentUnits() * s;
	}

	public float getLineHeightPx(float pixelSize) {
		float s = getScaleForPixelSize(pixelSize);
		return (getAscentUnits() - getDescentUnits() + getLineGapUnits()) * s;
	}

	public float getScaleForPixelSize(float pixelSize) {
		return pixelSize / (float) getUnitsPerEm();
	}

	// kerning

	public int getKerningUnits(int leftGid, int rightGid) {
		if (kernTable == null) {
			return 0;
		}
		return kernTable.getKerning(leftGid, rightGid);
	}

	// static helpers

	public static boolean isVariationSelector(int cp) {
		// VS1..VS16
		if (cp >= 0xFE00 && cp <= 0xFE0F) {
			return true;
		}
		// VS17..VS256 (supplementary plane)
		return (cp >= 0xE0100 && cp <= 0xE01EF);
	}
}

