package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.SneakyThrows;
import me.mdbell.awtea.classlib.java.awt.image.TFontFormatException;
import me.mdbell.awtea.font.TrueTypeFont;
import me.mdbell.awtea.impl.Debug;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.AttributedCharacterIterator;
import java.util.Map;
import java.util.Objects;

/**
 * @see java.awt.Font
 */
@Getter
public class TFont {

	private static final String TTF_DIR = "/fonts/";

	private static final String FALLBACK_FONT_NAME = "NotoSans";

	public static final int TRUETYPE_FONT = 0;

	public static final int TYPE1_FONT = 1;

    public static final int PLAIN = 0;
    public static final int BOLD = 1;
    public static final int ITALIC = 2;

	public static final int ROMAN_BASELINE = 0;
	public static final int CENTER_BASELINE = 1;
	public static final int HANGING_BASELINE = 2;

	public static final int LAYOUT_LEFT_TO_RIGHT = 0;
	public static final int LAYOUT_RIGHT_TO_LEFT = 1;
	public static final int LAYOUT_NO_START_CONTEXT = 2;
	public static final int LAYOUT_NO_LIMIT_CONTEXT = 4;

    private final String name;
    private final int style;
    private final int size;

	private final TrueTypeFont trueType;

	public static final String SERIF = "Serif";
	public static final String SANS_SERIF = "SansSerif";
	public static final String MONOSPACED = "Monospaced";
	public static final String DIALOG = "Dialog";
	public static final String DIALOG_INPUT = "DialogInput";

    public TFont(String name, int style, int size) {
		name = "NotoSans";
		style = 0;
        this.name = name;
        this.style = style;
        this.size = size;

		String styleStr = isBold() ? "Bold" : "";
		if(isItalic()) {
			styleStr += "Italic";
		}

		this.trueType = loadSafeFont(name, styleStr);
	}

	public TFont(Map<? extends AttributedCharacterIterator.Attribute,?> attributes) {
		throw Debug.unimplemented();
	}

	TFont(TrueTypeFont trueType, int size) {
		this.name = trueType.getFamily();
		this.trueType = trueType;
		this.size = size;

		int style = 0;
		if(trueType.isBold()){
			style |= BOLD;
		}
		if(trueType.isItalic()){
			style |= ITALIC;
		}
		if(style == 0){
			style = PLAIN;
		}
		this.style = style;
	}

	public static TFont getDefaultFont() {
		return new TFont("Helvetica", PLAIN, 12);
	}

	public String getFamily() {
		return trueType.getFamily();
	}

	public String getPSName(){
		return trueType.getPostScriptName();
	}

	public String getFontName() {
		return trueType.getFullName(); // this may not be right
	}

	public TFontMetrics getFontMetrics() {
		return new TFontMetrics(this);
	}

	public TFont deriveFont(int newStyle) {
		if (newStyle == this.style) return this;
		return new TFont(name, newStyle, size);
	}

	public TFont deriveFont(float newSize) {
		int s = Math.round(newSize);
		if (s == this.size) return this;
		return new TFont(name, style, s);
	}

	public TFont deriveFont(int newStyle, float newSize) {
		int s = Math.round(newSize);
		if (s == this.size && newStyle == this.style) return this;
		return new TFont(name, newStyle, s);
	}

	public boolean isBold() {
        return (style & BOLD) != 0;
    }

    public boolean isPlain() {
        return style == PLAIN;
    }

    public boolean isItalic() {
        return (style & ITALIC) != 0;
    }

	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof TFont)) return false;
		TFont tFont = (TFont) o;
		return style == tFont.style &&
			size == tFont.size &&
			Objects.equals(name, tFont.name) &&
			trueType == tFont.trueType; // identity compare; you typically only have one instance per file
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, style, size, System.identityHashCode(trueType));
	}

	public static TFont createFont(int fontFormat, InputStream in) throws TFontFormatException, IOException {
		if(fontFormat != TRUETYPE_FONT) {
			throw new TFontFormatException("Only TrueType fonts are supported");
		}
			byte[] fontData = in.readAllBytes();
			TrueTypeFont ttf = TrueTypeFont.read(fontData);
			return new TFont(ttf, 1); // default size
	}

	public static TFont createFont(int fontFormat, File fontFile) throws TFontFormatException, IOException {
		return createFont(fontFormat, Files.newInputStream(fontFile.toPath()));
	}

	/**
	 * Decodes fonts in one of the following formats:
	 * name-style-size
	 * name-size
	 * name-style
	 * name
	 * name style size
	 * name size
	 * name style
	 * name
	 * where style is a combination of "bold" and "italic", size is an integer.
	 *
	 * @param str the font string
	 * @return the font
	 */
	public static TFont decode(String str) {
		int style = PLAIN;
		int size = 12;

		if(str == null) {
			return new TFont(DIALOG, style, size);
		}

		int lastHyphen = str.lastIndexOf('-');
		int lastSpace = str.lastIndexOf(' ');
		String seperator = Character.toString((lastHyphen > lastSpace) ? '-' : ' ');

		String[] parts = str.split(seperator);
		if(parts.length >= 2) {
			// check if last part is size
			try {
				size = Integer.parseInt(parts[parts.length - 1]);
				parts = java.util.Arrays.copyOf(parts, parts.length - 1);
			} catch (NumberFormatException e) {
				// not a size
			}
		}
		if(parts.length >= 2) {
			// check if last part is style
			String stylePart = parts[parts.length - 1].toLowerCase();
			switch (stylePart) {
				case "bolditalic":
				case "italicbold":
					style = BOLD | ITALIC;
					parts = java.util.Arrays.copyOf(parts, parts.length - 1);
					break;
				case "bold":
					style = BOLD;
					parts = java.util.Arrays.copyOf(parts, parts.length - 1);
					break;
				case "italic":
					style = ITALIC;
					parts = java.util.Arrays.copyOf(parts, parts.length - 1);
					break;
				default:
					// not a style, so it's part of the name
					break;
			}
		}
		String name = String.join(seperator, parts);
		return new TFont(name, style, size);
	}

	public static TFont getFont(String nm, TFont defaultFont) {
		String property = System.getProperty(nm);
		if(property == null || property.isEmpty()) {
			return defaultFont;
		}
		return decode(property);
	}

	public static TFont getFont(String nm) {
		return getFont(nm, null);
	}

	@SneakyThrows
	private static TrueTypeFont loadSafeFont(String name, String style){
		String fontname = name + "-" + style;
		try {
			byte[] b = getFontBytes(fontname);
			return TrueTypeFont.read(b);
		} catch (IOException e) {
			System.err.println("Missing font:" + fontname + " - falling back");
			// shouldn't actually throw, so we sneaky throw
			return TrueTypeFont.read(getFontBytes(FALLBACK_FONT_NAME));
		}
	}

	private static byte[] getFontBytes(String name) throws IOException {
		String path = TTF_DIR + name + ".ttf";
		try(InputStream in = TFont.class.getResourceAsStream(path)) {
			if(in == null){
				throw new IOException("No font");
			}
			return in.readAllBytes();
		}
	}
}
