package me.mdbell.awtea.font;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

/**
 * Wraps another FontRenderer to apply synthetic bold/italic styling
 * when the actual font variant is not available.
 * 
 * <p>This renderer provides fallback styling when dedicated bold/italic
 * font files are missing. While not as high quality as real font variants,
 * it ensures text remains readable and visually distinct.</p>
 * 
 * <h3>Synthetic Bold</h3>
 * <p>Implemented by rendering each glyph multiple times with slight horizontal
 * offset. This creates a heavier appearance similar to bold text.</p>
 * 
 * <h3>Synthetic Italic</h3>
 * <p>Implemented by applying a shear transformation that slants the text.
 * This approximates oblique styling but lacks the design refinements of
 * true italic fonts.</p>
 * 
 * @see FontRenderer
 */
public class SyntheticStyledFontRenderer implements FontRenderer {
    
    private static final Logger log = LoggerFactory.getLogger(SyntheticStyledFontRenderer.class);
    
    private final FontRenderer delegate;
    private final boolean syntheticBold;
    private final boolean syntheticItalic;
    
    // Shear factor for synthetic italic (standard oblique angle ~11 degrees)
    private static final float ITALIC_SHEAR = -0.2f;
    
    // Offset for synthetic bold (render multiple times with slight offset)
    private static final int BOLD_OFFSET_PIXELS = 1;
    
    /**
     * Create a synthetic styled font renderer.
     * 
     * @param delegate the underlying renderer to delegate to
     * @param syntheticBold whether to apply synthetic bold
     * @param syntheticItalic whether to apply synthetic italic
     */
    public SyntheticStyledFontRenderer(FontRenderer delegate, 
                                       boolean syntheticBold, 
                                       boolean syntheticItalic) {
        this.delegate = delegate;
        this.syntheticBold = syntheticBold;
        this.syntheticItalic = syntheticItalic;
        
        if (syntheticBold || syntheticItalic) {
            log.debug("Synthetic styling enabled: bold={}, italic={}", syntheticBold, syntheticItalic);
        }
    }
    
    @Override
    public void renderGlyph(TrueTypeFont font, int glyphId, RasterTarget target,
                           float sizePx, int x, int y, int argb) {
        int offsetX = x;
        
        // Apply italic shear - shift x based on y position
        if (syntheticItalic) {
            // Calculate offset based on distance from baseline
            // For italic, shift increases as we go up from baseline
            int italicOffsetX = (int)(y * ITALIC_SHEAR);
            offsetX += italicOffsetX;
        }
        
        if (syntheticBold) {
            // Render glyph multiple times with slight offsets for bold effect
            delegate.renderGlyph(font, glyphId, target, sizePx, offsetX, y, argb);
            delegate.renderGlyph(font, glyphId, target, sizePx, offsetX + BOLD_OFFSET_PIXELS, y, argb);
        } else {
            delegate.renderGlyph(font, glyphId, target, sizePx, offsetX, y, argb);
        }
    }
    
    @Override
    public void renderString(TrueTypeFont font, String text, RasterTarget target,
                            float sizePx, int x, int y, int argb) {
        // For string rendering with synthetic styling, we need to apply per-glyph transformations
        // Delegate to renderGlyph for each character to properly apply italic shear
        if (syntheticBold || syntheticItalic) {
            int currentX = x;
            float scale = sizePx / font.getUnitsPerEm();
            
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                int codePoint = (int) c;
                int glyphId = font.glyphForCodePoint(codePoint);
                
                if (glyphId != 0) {
                    renderGlyph(font, glyphId, target, sizePx, currentX, y, argb);
                    
                    // Advance position
                    int advanceWidth = font.getAdvanceWidthUnits(glyphId);
                    currentX += (int)(advanceWidth * scale);
                    
                    // Add extra advance for bold
                    if (syntheticBold) {
                        currentX += BOLD_OFFSET_PIXELS;
                    }
                }
            }
        } else {
            // No synthetic styling needed, use delegate directly
            delegate.renderString(font, text, target, sizePx, x, y, argb);
        }
    }
    
    @Override
    public int measureString(TrueTypeFont font, String text, float sizePx) {
        int width = delegate.measureString(font, text, sizePx);
        
        // Bold makes text slightly wider due to extra rendering passes
        if (syntheticBold) {
            width += text.length() * BOLD_OFFSET_PIXELS;
        }
        
        return width;
    }
    
    @Override
    public void clearCache() {
        delegate.clearCache();
    }
}
