package me.mdbell.awtea.font;

/**
 * A peer that manages font rendering strategy selection and execution.
 *
 * <p>This abstraction, inspired by AWT's {@code java.awt.peer.FontPeer}, provides
 * a layer of indirection between the logical font (represented by TFont) and
 * the actual rendering implementation. This design enables:
 *
 * <ul>
 *   <li>Backend-independent font management</li>
 *   <li>Pluggable rendering strategies (rasterization, SDF, canvas, etc.)</li>
 *   <li>Simplified testing and benchmarking of different approaches</li>
 *   <li>Easy addition of new rendering backends</li>
 * </ul>
 *
 * <p>The FontPeer holds a reference to both the font data ({@link TrueTypeFont})
 * and the rendering strategy ({@link FontRenderer}), coordinating between them
 * to provide a complete font rendering solution.
 *
 * @see FontRenderer
 * @see TrueTypeFont
 */
public class FontPeer {

    private final TrueTypeFont font;
    private final FontRenderer renderer;

    /**
     * Create a new FontPeer with the specified font data and renderer.
     *
     * @param font     the TrueType font data
     * @param renderer the rendering strategy to use
     */
    public FontPeer(TrueTypeFont font, FontRenderer renderer) {
        this.font = font;
        this.renderer = renderer;
    }

    /**
     * Get the underlying TrueType font data.
     *
     * @return the font data
     */
    public TrueTypeFont getFont() {
        return font;
    }

    /**
     * Get the renderer used by this peer.
     *
     * @return the font renderer
     */
    public FontRenderer getRenderer() {
        return renderer;
    }

    /**
     * Render a string using this font peer.
     *
     * @param text   the text to render
     * @param target the target surface
     * @param sizePx the font size in pixels
     * @param x      the x-coordinate
     * @param y      the y-coordinate
     * @param argb   the color in ARGB format
     */
    public void renderString(String text, FontRenderer.RasterTarget target,
                             float sizePx, int x, int y, int argb) {
        renderer.renderString(font, text, target, sizePx, x, y, argb);
    }

    /**
     * Measure the width of a text string.
     *
     * @param text   the text to measure
     * @param sizePx the font size in pixels
     * @return the width in pixels
     */
    public int measureString(String text, float sizePx) {
        return renderer.measureString(font, text, sizePx);
    }

    /**
     * Get font metrics for the specified size.
     *
     * @param sizePx the font size in pixels
     * @return font metrics
     */
    public FontMetrics getFontMetrics(float sizePx) {
        return new FontMetrics(
                font.getAscentPx(sizePx),
                font.getDescentPx(sizePx),
                font.getLineHeightPx(sizePx)
        );
    }

    /**
     * Font metrics data.
     */
    public static class FontMetrics {
        private final float ascent;
        private final float descent;
        private final float lineHeight;

        public FontMetrics(float ascent, float descent, float lineHeight) {
            this.ascent = ascent;
            this.descent = descent;
            this.lineHeight = lineHeight;
        }

        public float getAscent() {
            return ascent;
        }

        public float getDescent() {
            return descent;
        }

        public float getLineHeight() {
            return lineHeight;
        }
    }
}
