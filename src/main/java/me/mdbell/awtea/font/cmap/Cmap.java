package me.mdbell.awtea.font.cmap;

public interface Cmap {
	/**
	 * Map a Unicode code point to a glyph ID.
	 * Returns 0 if the character is unmapped.
	 */
	int mapCodePointToGlyph(int codePoint);
}
