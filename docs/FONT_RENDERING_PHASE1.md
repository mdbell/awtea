# Font Rendering Modernization - Phase 1: Core Metrics

This document describes the Phase 1 changes to bring awtea's font rendering API in line with AWT standards.

## Overview

Phase 1 focuses on making font metrics context-aware by introducing proper `FontRenderContext` support and modernizing the `FontMetrics` API to match AWT behavior.

## Key Changes

### 1. FontRenderContext Support

**`TFontRenderContext`** is now fully functional and encapsulates:
- Transform (for scaled/rotated text)
- Anti-aliasing setting
- Fractional metrics setting

**`TGraphics2D.getFontRenderContext()`** has been implemented to extract the current rendering context from Graphics2D, including:
- Current transform
- Anti-aliasing hints (`KEY_TEXT_ANTIALIASING` or `KEY_ANTIALIASING`)
- Fractional metrics hints (`KEY_FRACTIONALMETRICS`)

### 2. Context-Aware Font Metrics

**`TFontMetrics`** now:
- Takes a `TFontRenderContext` in its constructor
- Stores both integer and float metric values
- Applies rounding based on fractional metrics setting:
  - **With fractional metrics**: rounds to nearest pixel
  - **Without fractional metrics**: truncates (traditional behavior)

**Migration Path:**
```java
// OLD (deprecated):
TFontMetrics metrics = font.getFontMetrics();

// NEW (preferred):
TFontMetrics metrics = graphics.getFontMetrics(font);
// or
TFontMetrics metrics = toolkit.getFontMetrics(font);
```

### 3. LineMetrics Support

**`TLineMetrics`** (abstract base class) and **`TSimpleLineMetrics`** (concrete implementation) provide:
- Character count
- Ascent, descent, leading (float values)
- Height calculation
- Baseline information (index and offsets)
- Underline position and thickness
- Strikethrough position and thickness

**Usage:**
```java
TFontRenderContext frc = graphics.getFontRenderContext();
TLineMetrics lineMetrics = font.getLineMetrics("Hello World", frc);

float ascent = lineMetrics.getAscent();
float descent = lineMetrics.getDescent();
float height = lineMetrics.getHeight();
```

### 4. Enhanced FontMetrics API

New methods added to `TFontMetrics` to match AWT:

**Float-valued methods:**
- `getStringBounds(String)` - Returns `TRectangle2D` bounds
- `getStringBounds(String, TGraphics)` - Context-aware bounds
- `getStringBounds(CharacterIterator, int, int)` - Iterator-based bounds
- `getStringBounds(char[], int, int)` - Array-based bounds

**LineMetrics methods:**
- `getLineMetrics(String)` - Simple line metrics
- `getLineMetrics(String, TGraphics)` - Context-aware line metrics
- `getLineMetrics(CharacterIterator, int, int)` - Iterator-based
- `getLineMetrics(char[], int, int)` - Array-based

**Additional methods:**
- `getMaxAscent()` - Maximum ascent
- `getMaxDescent()` - Maximum descent
- `getMaxAdvance()` - Maximum character advance width

### 5. TFont Enhancements

**New `getLineMetrics()` methods:**
```java
// Basic usage
TLineMetrics lm = font.getLineMetrics("Text", frc);

// With substring
TLineMetrics lm = font.getLineMetrics("Text", beginIndex, limit, frc);

// With CharacterIterator
TLineMetrics lm = font.getLineMetrics(iterator, beginIndex, limit, frc);

// With character array
TLineMetrics lm = font.getLineMetrics(chars, beginIndex, limit, frc);
```

## Rendering Context Behavior

### Graphics Context
When you call `graphics.getFontMetrics(font)`:
1. Graphics2D extracts current transform
2. Checks rendering hints for anti-aliasing and fractional metrics
3. Creates a `TFontRenderContext` with these settings
4. Creates `TFontMetrics` with this context

### Toolkit Context
When you call `toolkit.getFontMetrics(font)`:
- Creates a default context (no AA, no fractional metrics)
- Provides baseline metrics suitable for layout calculations

## Fractional Metrics Impact

**Without fractional metrics (default):**
```java
float ascentPx = 10.7f;
int ascent = (int) ascentPx;  // 10 (truncated)
```

**With fractional metrics:**
```java
float ascentPx = 10.7f;
int ascent = Math.round(ascentPx);  // 11 (rounded)
```

This affects:
- Line height calculations
- Text positioning accuracy
- Overall text rendering quality

## Deprecations

### `TFont.getFontMetrics()`
**Deprecated** because it creates metrics without rendering context awareness.

**Why deprecated:**
- Doesn't reflect actual rendering conditions
- Can't account for anti-aliasing or fractional metrics
- Not consistent with AWT design

**Migration:**
```java
// Instead of:
TFontMetrics metrics = font.getFontMetrics();

// Use:
TFontMetrics metrics = graphics.getFontMetrics(font);
```

## Testing

Comprehensive tests added in `FontRenderingContextTest.java`:
- FontRenderContext creation and equality
- Context-aware metrics creation
- Basic metrics methods (ascent, descent, leading, height)
- String width measurements
- LineMetrics functionality
- Baseline calculations
- Underline/strikethrough metrics
- String bounds calculations
- Toolkit integration

## Future Enhancements (Phase 2+)

Phase 1 provides the foundation. Future phases will add:
- Advanced glyph metrics
- Character-specific measurements
- Kerning support
- More sophisticated transform handling
- Advanced rendering hints

## References

- [AWT FontMetrics Javadoc](https://docs.oracle.com/javase/8/docs/api/java/awt/FontMetrics.html)
- [AWT FontRenderContext Javadoc](https://docs.oracle.com/javase/8/docs/api/java/awt/font/FontRenderContext.html)
- [AWT LineMetrics Javadoc](https://docs.oracle.com/javase/8/docs/api/java/awt/font/LineMetrics.html)
