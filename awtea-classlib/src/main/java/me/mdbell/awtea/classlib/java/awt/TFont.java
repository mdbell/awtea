package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.SneakyThrows;
import me.mdbell.awtea.classlib.java.awt.image.TFontFormatException;
import me.mdbell.awtea.font.FontLoader;
import me.mdbell.awtea.font.FontPeer;
import me.mdbell.awtea.font.FontRenderer;
import me.mdbell.awtea.font.FontRendererFactory;
import me.mdbell.awtea.font.TrueTypeFont;
import me.mdbell.awtea.impl.Debug;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(TFont.class);

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

    /**
     * -- GETTER --
     * Get the FontPeer for this font, which provides access to rendering capabilities.
     */
    @Getter
    private final FontPeer fontPeer;

    public static final String SERIF = "Serif";
    public static final String SANS_SERIF = "SansSerif";
    public static final String MONOSPACED = "Monospaced";
    public static final String DIALOG = "Dialog";
    public static final String DIALOG_INPUT = "DialogInput";

    public TFont(String name, int style, int size) {
        this.name = name;
        this.style = style;
        this.size = size;

        String styleStr = isBold() ? "Bold" : "";
        if (isItalic()) {
            styleStr += "Italic";
        }

        TrueTypeFont trueType;
        boolean needsSyntheticBold = false;
        boolean needsSyntheticItalic = false;
        
        // Try to load styled font
        try {
            trueType = loadStyledFont(name, styleStr);
            
            // Check if loaded font actually has the requested style
            // (some fonts may load but not have the style embedded)
            if (isBold() && !trueType.isBold()) {
                needsSyntheticBold = true;
            }
            if (isItalic() && !trueType.isItalic()) {
                needsSyntheticItalic = true;
            }
        } catch (RuntimeException e) {
            // Styled font doesn't exist, fall back to plain with synthetic styling
            if (!styleStr.isEmpty()) {
                log.debug("Styled font '{}{}' not found, using synthetic styling: {}", 
                         name, styleStr, e.getMessage());
                trueType = loadSafeFont(name, "");  // Load plain font
                needsSyntheticBold = isBold();
                needsSyntheticItalic = isItalic();
            } else {
                // Even plain font failed, propagate error
                throw e;
            }
        }
        
        FontRenderer renderer = FontRendererFactory.getDefaultRenderer();
        
        // Wrap with synthetic styling if needed
        if (needsSyntheticBold || needsSyntheticItalic) {
            renderer = new me.mdbell.awtea.font.SyntheticStyledFontRenderer(
                renderer, 
                needsSyntheticBold, 
                needsSyntheticItalic
            );
        }
        
        this.fontPeer = new FontPeer(trueType, renderer);
    }

    public TFont(Map<? extends AttributedCharacterIterator.Attribute, ?> attributes) {
        throw Debug.unimplemented();
    }

    TFont(TrueTypeFont trueType, int size) {
        this.name = trueType.getFamily();
        this.size = size;

        int style = PLAIN;
        if (trueType.isBold()) {
            style |= BOLD;
        }
        if (trueType.isItalic()) {
            style |= ITALIC;
        }

        this.style = style;
        this.fontPeer = new FontPeer(trueType, FontRendererFactory.getDefaultRenderer());
    }

    public static TFont getDefaultFont() {
        return new TFont("Helvetica", PLAIN, 12);
    }

    public String getFamily() {
        return fontPeer.getFont().getFamily();
    }

    public String getPSName() {
        return fontPeer.getFont().getPostScriptName();
    }

    public String getFontName() {
        return fontPeer.getFont().getFullName(); // this may not be right
    }

    /**
     * Get the underlying TrueType font data.
     *
     * @return the TrueType font
     * @deprecated Use getFontPeer().getFont() instead for better encapsulation
     */
    @Deprecated
    public TrueTypeFont getTrueType() {
        return fontPeer.getFont();
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
                fontPeer.getFont() == tFont.fontPeer.getFont(); // identity compare; you typically only have one instance per file
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, style, size, System.identityHashCode(fontPeer.getFont()));
    }

    public static TFont createFont(int fontFormat, InputStream in) throws TFontFormatException, IOException {
        if (fontFormat != TRUETYPE_FONT) {
            log.error("Unsupported font format: {}", fontFormat);
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

        if (str == null) {
            return new TFont(DIALOG, style, size);
        }

        int lastHyphen = str.lastIndexOf('-');
        int lastSpace = str.lastIndexOf(' ');
        String seperator = Character.toString((lastHyphen > lastSpace) ? '-' : ' ');

        String[] parts = str.split(seperator);
        if (parts.length >= 2) {
            // check if last part is size
            try {
                size = Integer.parseInt(parts[parts.length - 1]);
                parts = java.util.Arrays.copyOf(parts, parts.length - 1);
            } catch (NumberFormatException e) {
                // not a size
            }
        }
        if (parts.length >= 2) {
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
        if (property == null || property.isEmpty()) {
            return defaultFont;
        }
        return decode(property);
    }

    public static TFont getFont(String nm) {
        return getFont(nm, null);
    }

    /**
     * Load a font with a specific style, throwing an exception if not found.
     * This method is used to detect missing font variants so synthetic styling can be applied.
     * 
     * @param name the font family name
     * @param style the style string (e.g., "Bold", "Italic", "BoldItalic", or empty for plain)
     * @return the loaded TrueTypeFont
     * @throws RuntimeException if the font variant is not found
     */
    @SneakyThrows
    private static TrueTypeFont loadStyledFont(String name, String style) {
        String fontname = name;
        if (style != null && !style.isEmpty()) {
            fontname = name + "-" + style;
        }
        try {
            return FontLoader.loadFont(fontname);
        } catch (IOException e) {
            // Rethrow so caller can detect missing variant and apply synthetic styling
            throw new RuntimeException("Font variant not found: " + fontname, e);
        }
    }

    /**
     * Load a font safely with fallback to default font if not found.
     * This method never throws - it always returns a valid font.
     * 
     * @param name the font family name
     * @param style the style string (e.g., "Bold", "Italic", "BoldItalic", or empty for plain)
     * @return the loaded TrueTypeFont, or fallback font if not found
     */
    @SneakyThrows
    private static TrueTypeFont loadSafeFont(String name, String style) {
        String fontname = name;
        if (style != null && !style.isEmpty()) {
            fontname = name + "-" + style;
        }
        try {
            return FontLoader.loadFont(fontname);
        } catch (IOException e) {
            log.warn("Missing font:{}", fontname + " - falling back");
            try {
                return FontLoader.loadFont(FALLBACK_FONT_NAME);
            } catch (IOException fallbackError) {
                // If even fallback fails, try legacy resource loading as last resort
                log.error("Fallback font also failed, trying legacy resource loading");
                return TrueTypeFont.read(getFontBytes(FALLBACK_FONT_NAME));
            }
        }
    }

    private static byte[] getFontBytes(String name) throws IOException {
        String path = TTF_DIR + name + ".ttf";
        try (InputStream in = TFont.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("No font");
            }
            return in.readAllBytes();
        }
    }
}
