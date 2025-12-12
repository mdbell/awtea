# Class: `Font` ![Coverage](https://img.shields.io/badge/coverage-45.8%25-orange)

**Full Name:** `java.awt.Font`

**Coverage:** 38 / 83 (45.8%)

```
[██████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 45.8%
```

## ✓ Implemented Methods

- `public boolean equals(java.lang.Object)`
- `public boolean isBold()`
- `public boolean isItalic()`
- `public boolean isPlain()`
- `public int getSize()`
- `public int getStyle()`
- `public int hashCode()`
- `public java.awt.Font deriveFont(float)`
- `public java.awt.Font deriveFont(int)`
- `public java.awt.Font deriveFont(int, float)`
- `public java.lang.String getFamily()`
- `public java.lang.String getFontName()`
- `public java.lang.String getName()`
- `public java.lang.String getPSName()`
- `public static java.awt.Font createFont(int, java.io.File)`
- `public static java.awt.Font createFont(int, java.io.InputStream)`
- `public static java.awt.Font decode(java.lang.String)`
- `public static java.awt.Font getFont(java.lang.String)`
- `public static java.awt.Font getFont(java.lang.String, java.awt.Font)`

## ✗ Missing Methods

- `public boolean canDisplay(char)`
- `public boolean canDisplay(int)`
- `public boolean hasLayoutAttributes()`
- `public boolean hasUniformLineMetrics()`
- `public boolean isTransformed()`
- `public byte getBaselineFor(char)`
- `public float getItalicAngle()`
- `public float getSize2D()`
- `public int canDisplayUpTo(char[], int, int)`
- `public int canDisplayUpTo(java.lang.String)`
- `public int canDisplayUpTo(java.text.CharacterIterator, int, int)`
- `public int getMissingGlyphCode()`
- `public int getNumGlyphs()`
- `public java.awt.Font deriveFont(int, java.awt.geom.AffineTransform)`
- `public java.awt.Font deriveFont(java.awt.geom.AffineTransform)`
- `public java.awt.Font deriveFont(java.util.Map)`
- `public java.awt.font.GlyphVector createGlyphVector(java.awt.font.FontRenderContext, char[])`
- `public java.awt.font.GlyphVector createGlyphVector(java.awt.font.FontRenderContext, int[])`
- `public java.awt.font.GlyphVector createGlyphVector(java.awt.font.FontRenderContext, java.lang.String)`
- `public java.awt.font.GlyphVector createGlyphVector(java.awt.font.FontRenderContext, java.text.CharacterIterator)`
- `public java.awt.font.GlyphVector layoutGlyphVector(java.awt.font.FontRenderContext, char[], int, int, int)`
- `public java.awt.font.LineMetrics getLineMetrics(char[], int, int, java.awt.font.FontRenderContext)`
- `public java.awt.font.LineMetrics getLineMetrics(java.lang.String, int, int, java.awt.font.FontRenderContext)`
- `public java.awt.font.LineMetrics getLineMetrics(java.lang.String, java.awt.font.FontRenderContext)`
- `public java.awt.font.LineMetrics getLineMetrics(java.text.CharacterIterator, int, int, java.awt.font.FontRenderContext)`
- `public java.awt.geom.AffineTransform getTransform()`
- `public java.awt.geom.Rectangle2D getMaxCharBounds(java.awt.font.FontRenderContext)`
- `public java.awt.geom.Rectangle2D getStringBounds(char[], int, int, java.awt.font.FontRenderContext)`
- `public java.awt.geom.Rectangle2D getStringBounds(java.lang.String, int, int, java.awt.font.FontRenderContext)`
- `public java.awt.geom.Rectangle2D getStringBounds(java.lang.String, java.awt.font.FontRenderContext)`
- `public java.awt.geom.Rectangle2D getStringBounds(java.text.CharacterIterator, int, int, java.awt.font.FontRenderContext)`
- `public java.lang.String getFamily(java.util.Locale)`
- `public java.lang.String getFontName(java.util.Locale)`
- `public java.lang.String toString()`
- `public java.text.AttributedCharacterIterator$Attribute[] getAvailableAttributes()`
- `public java.util.Map getAttributes()`
- `public static boolean textRequiresLayout(char[], int, int)`
- `public static java.awt.Font getFont(java.util.Map)`
- `public static java.awt.Font[] createFonts(java.io.File)`
- `public static java.awt.Font[] createFonts(java.io.InputStream)`

## ✓ Implemented Fields

- `public static final int BOLD`
- `public static final int CENTER_BASELINE`
- `public static final int HANGING_BASELINE`
- `public static final int ITALIC`
- `public static final int LAYOUT_LEFT_TO_RIGHT`
- `public static final int LAYOUT_NO_LIMIT_CONTEXT`
- `public static final int LAYOUT_NO_START_CONTEXT`
- `public static final int LAYOUT_RIGHT_TO_LEFT`
- `public static final int PLAIN`
- `public static final int ROMAN_BASELINE`
- `public static final int TRUETYPE_FONT`
- `public static final int TYPE1_FONT`
- `public static final java.lang.String DIALOG`
- `public static final java.lang.String DIALOG_INPUT`
- `public static final java.lang.String MONOSPACED`
- `public static final java.lang.String SANS_SERIF`
- `public static final java.lang.String SERIF`

## ✗ Missing Fields

- `protected float pointSize`
- `protected int size`
- `protected int style`
- `protected java.lang.String name`

## ✓ Implemented Constructors

- `public java.awt.Font(java.lang.String, int, int)`
- `public java.awt.Font(java.util.Map)`

## ✗ Missing Constructors

- `protected java.awt.Font(java.awt.Font)`


[← Back to Package](java_awt.md)
