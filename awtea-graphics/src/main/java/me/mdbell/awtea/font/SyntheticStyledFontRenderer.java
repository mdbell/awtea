package me.mdbell.awtea.font;

import me.mdbell.awtea.util.GlyphRasterizer;
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
 * <p>Implemented by applying a shear transformation at the glyph rasterization level.
 * This creates an oblique slant that approximates italic styling but lacks the design 
 * refinements of true italic fonts.</p>
 * 
 * @see FontRenderer
 */
public class SyntheticStyledFontRenderer implements FontRenderer {
    
    private static final Logger log = LoggerFactory.getLogger(SyntheticStyledFontRenderer.class);
    
    private final FontRenderer delegate;
    private final boolean syntheticBold;
    private final boolean syntheticItalic;
    
    // Shear factor for synthetic italic (approximately 7-8 degrees for better readability)
    // A more conservative angle than the typical 11-12 degrees provides better readability
    // in synthetic italic, especially at smaller sizes
    private static final float ITALIC_SHEAR = -0.15f;
    
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
        // Use GlyphRasterizer directly with shear for italic
        if (syntheticItalic || syntheticBold) {
            GlyphRasterizer.RasterTarget adaptedTarget = adaptTarget(target);
            float shear = syntheticItalic ? ITALIC_SHEAR : 0.0f;
            
            if (syntheticBold) {
                // Render glyph multiple times with slight offsets for bold effect
                GlyphRasterizer.drawGlyph(font, glyphId, adaptedTarget, sizePx, x, y, argb, 4, false, shear);
                GlyphRasterizer.drawGlyph(font, glyphId, adaptedTarget, sizePx, x + BOLD_OFFSET_PIXELS, y, argb, 4, false, shear);
            } else {
                GlyphRasterizer.drawGlyph(font, glyphId, adaptedTarget, sizePx, x, y, argb, 4, false, shear);
            }
        } else {
            delegate.renderGlyph(font, glyphId, target, sizePx, x, y, argb);
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
                } else {
                    // Missing glyph - log warning for debugging
                    log.debug("Missing glyph for codePoint: {} (char: '{}')", codePoint, c);
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
    
    /**
     * Adapt FontRenderer.RasterTarget to GlyphRasterizer.RasterTarget.
     */
    private GlyphRasterizer.RasterTarget adaptTarget(RasterTarget target) {
        return new GlyphRasterizer.RasterTarget() {
            @Override
            public int getWidth() {
                return target.getWidth();
            }
            
            @Override
            public int getHeight() {
                return target.getHeight();
            }
            
            @Override
            public void setRGB(int x, int y, int argb) {
                target.setRGB(x, y, argb);
            }
            
            @Override
            public int getRGB(int x, int y) {
                return target.getRGB(x, y);
            }
        };
    }
}
