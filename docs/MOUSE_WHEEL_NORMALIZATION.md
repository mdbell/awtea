# Mouse Wheel Normalization Testing Guide

This guide explains how to test the mouse wheel scroll delta normalization feature in AWTea.

## Overview

The mouse wheel normalization feature addresses the issue where browser wheel deltas are much larger than what Java AWT expects, causing excessively fast scrolling. The implementation normalizes browser wheel events using configurable divisors based on the browser's `deltaMode`.

## What Was Implemented

### Code Changes

1. **TEventManager.java** - Core normalization logic:
   - Added `normalizeWheelDelta()` method to normalize raw browser deltas
   - Added `getWheelDivisor()` method to retrieve configurable divisors from system properties
   - Modified `withMouseWheel()` to apply normalization before creating events
   - Added trace logging for debugging wheel events

2. **System Properties** - Three new configurable properties:
   - `me.mdbell.awtea.mouseWheel.pixelDivisor` (default: 100)
   - `me.mdbell.awtea.mouseWheel.lineDivisor` (default: 3)
   - `me.mdbell.awtea.mouseWheel.pageMultiplier` (default: 1)

3. **Documentation**:
   - Updated `docs/SYSTEM_PROPERTIES.md` with detailed property documentation
   - Added test suite in `MouseWheelNormalizationTests.java`
   - Created demo panel in `MouseWheelDemoPanel.java`

### Default Behavior

Without any configuration, the normalization works as follows:

- **PIXEL mode (deltaMode = 0)**: Raw delta ÷ 100
  - Example: Chrome's 100 pixels per notch → 1.0 normalized
- **LINE mode (deltaMode = 1)**: Raw delta ÷ 3
  - Example: 3 lines per notch → 1.0 normalized
- **PAGE mode (deltaMode = 2)**: Raw delta × 1 (pass-through)
  - Example: 1 page per event → 1.0 normalized

## Testing the Implementation

### Manual Testing with Demo Panel

The `MouseWheelDemoPanel` class provides a visual test interface:

1. **Build the gui-demo example**:
   ```bash
   ./gradlew :examples:gui-demo:build
   ```

2. **Run the example** and observe:
   - Event count increments on each wheel event
   - Wheel rotation shows -1, 0, or 1
   - Precise rotation shows the normalized delta value
   - Delta mode shows PIXEL (0), LINE (1), or PAGE (2)
   - Scroll position updates based on normalized values
   - Blue indicator moves smoothly in the bar

3. **Test different configurations**:
   ```bash
   # Slower scrolling (larger divisor)
   -Dme.mdbell.awtea.mouseWheel.pixelDivisor=200
   
   # Faster scrolling (smaller divisor)
   -Dme.mdbell.awtea.mouseWheel.pixelDivisor=50
   
   # Custom line mode divisor
   -Dme.mdbell.awtea.mouseWheel.lineDivisor=5
   ```

### Browser Testing Checklist

Test in multiple browsers to ensure consistent behavior:

- [ ] **Chrome/Edge** (typically uses PIXEL mode, ~100 per notch)
  - Verify scrolling feels similar to native Java apps
  - Check that one notch ≈ one unit of movement
  
- [ ] **Firefox** (may use LINE or PIXEL mode)
  - Verify scrolling speed is comfortable
  - Adjust `lineDivisor` if using LINE mode
  
- [ ] **Safari** (macOS/iOS)
  - Test smooth scrolling vs notched scrolling
  - Verify trackpad gestures work reasonably

### Performance Testing

1. **Enable trace logging** to see raw vs normalized values:
   ```bash
   -Dme.mdbell.awtea.log.level=TRACE
   ```

2. **Check console output** for entries like:
   ```
   Mouse wheel: rawDelta=100.0, deltaMode=0, normalized=1.0, rotation=1
   ```

3. **Verify**:
   - Raw delta values match browser expectations
   - Normalized values are reasonable (~1.0 per notch)
   - Rotation is correctly -1, 0, or 1

## Tuning for Your Environment

### Finding the Right Divisor

If scrolling feels too fast or slow:

1. **Enable trace logging** to see raw delta values
2. **Scroll once** and note the `rawDelta` value
3. **Calculate ideal divisor**: `rawDelta ÷ desired_normalized_value`
   - For "1 unit per notch": use raw delta as divisor
   - For "2 units per notch": use (raw delta ÷ 2) as divisor

Example:
```
# If browser emits 120 pixels per notch but you want 1 unit:
-Dme.mdbell.awtea.mouseWheel.pixelDivisor=120

# If browser emits 100 pixels but you want 2 units per notch:
-Dme.mdbell.awtea.mouseWheel.pixelDivisor=50
```

### Platform-Specific Recommendations

**Windows**:
- Chrome: Default 100 works well
- Firefox: May need lineDivisor adjustment

**macOS**:
- Safari: Test with trackpad (smooth scroll) and mouse (notched)
- Chrome: May emit different pixel values

**Linux**:
- Test with your specific browser and desktop environment
- May need custom divisors per browser

## Troubleshooting

### Scrolling is too fast
- **Increase** the divisor (e.g., from 100 to 150 or 200)
- Check browser's deltaMode in logs - might be using unexpected mode

### Scrolling is too slow
- **Decrease** the divisor (e.g., from 100 to 50 or 75)
- Verify browser is emitting expected delta values

### Scrolling speed varies between browsers
- Set browser-specific divisors in your application
- Use JavaScript detection to set appropriate properties per browser

### Events not being normalized
- Verify TEventManager.withMouseWheel() is being called
- Check that mouse wheel listeners are properly registered
- Enable TRACE logging to see normalization in action

## Architecture Notes

### Why This Approach?

1. **Browser wheel events vary widely**: Chrome might emit 100 pixels/notch, Firefox might emit 3 lines/notch
2. **Java AWT expects small values**: Typically ±1 or ±3 per notch
3. **System properties enable tuning**: Users can adjust without code changes
4. **Three deltaMode types**: PIXEL (0), LINE (1), PAGE (2) need different handling

### Implementation Details

- Normalization happens in `TEventManager.withMouseWheel()` before creating `TMouseWheelEvent`
- The `preciseWheelRotation` field contains the normalized delta
- The `wheelRotation` field contains the sign (-1, 0, 1) of the normalized delta
- The `unitsToScroll` field is calculated as `rotation × SCROLL_AMOUNT`

### Future Enhancements

Potential improvements:
- Auto-detect browser and platform to choose optimal defaults
- Use browser APIs (if available) to query user's OS scroll settings
- Provide runtime UI for adjusting divisors without restart
- Store user preferences in localStorage/IndexedDB

## See Also

- [SYSTEM_PROPERTIES.md](../../docs/SYSTEM_PROPERTIES.md) - Complete property reference
- [TEventManager.java](../../awtea-classlib/src/main/java/me/mdbell/awtea/classlib/java/awt/awtea/TEventManager.java) - Implementation
- [MouseWheelNormalizationTests.java](../../awtea-classlib/src/test/java/me/mdbell/awtea/classlib/java/awt/test/MouseWheelNormalizationTests.java) - Test suite
