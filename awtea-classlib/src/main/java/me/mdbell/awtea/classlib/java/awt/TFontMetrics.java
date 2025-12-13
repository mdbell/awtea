package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import me.mdbell.awtea.font.TrueTypeFont;

/**
 * @see java.awt.FontMetrics
 */
@Getter
public class TFontMetrics {

    private final TFont font;
	private final int ascent;
	private final int descent;
	private final int leading;
	private final int lineHeight;

	TFontMetrics(TFont font){
		this.font = font;
		TrueTypeFont ttf = font.getFontPeer().getFont();

		float size = font.getSize();
		float ascent = ttf.getAscentPx(size);
		float descent = ttf.getDescentPx(size);
		float leading = descent * .2f;

		this.ascent = (int) ascent;
		this.descent =(int) descent;
		this.leading = (int) (descent * .2f);
		this.lineHeight = (int) leading;
	}

    public int stringWidth(String str) {
        return font.getFontPeer().measureString(str, font.getSize());
    }
}
