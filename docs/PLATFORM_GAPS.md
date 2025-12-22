# Platform Gaps and Browser Limitations

This document describes the known limitations and gaps in awtea's implementation of Java AWT/Swing/JRE APIs when running in browser environments via TeaVM.

## Overview

awtea provides browser-compatible implementations of Java AWT/Swing APIs, but some classes and methods cannot be fully implemented due to fundamental browser security and platform restrictions. This document lists all such gaps and the workarounds provided.

## File System and I/O Limitations

### `java.io.File.toPath()` / `java.nio.file.Path`
- **Status**: Stub via DetourHacks
- **Reason**: NIO.2 file API (java.nio.file.*) is not available in browser environments
- **Behavior**: Throws `UnsupportedOperationException` with clear error message
- **Workaround**: Use `java.io.File` APIs directly instead of Path-based APIs
- **Implementation**: `FileDetour.java`

### `java.io.RandomAccessFile.getChannel()` / `java.nio.channels.FileChannel`
- **Status**: Not implemented (skipped)
- **Reason**: NIO channels are not supported in browser environments
- **Workaround**: Use `RandomAccessFile` read/write methods directly

## Desktop Integration

### `java.awt.Desktop`
- **Status**: Stub implementation (TDesktop.java)
- **Reason**: Desktop operations (browse, open, mail, etc.) cannot be performed from browser-based applications
- **Behavior**: 
  - `isDesktopSupported()` returns `false`
  - `getDesktop()` throws `UnsupportedOperationException`
- **Workaround**: Use HTML5 APIs directly (window.open, mailto: links, etc.)
- **Implementation**: `TDesktop.java`

## Image Processing

### `javax.imageio.ImageIO`
- **Status**: Partial stub implementation (TImageIO.java)
- **Reason**: Limited file system access and different browser image APIs
- **Behavior**: 
  - `read(File)`, `read(InputStream)`, `read(URL)` throw `UnsupportedOperationException`
  - `write()` throws `UnsupportedOperationException` (cannot write files in browser)
- **Workaround**: Use `TImage` or browser's Image API with URLs/data URLs
- **Implementation**: `TImageIO.java`

### `java.awt.MediaTracker`
- **Status**: No-op stub implementation (TMediaTracker.java)
- **Reason**: Browser handles image loading asynchronously
- **Behavior**: 
  - All methods are no-ops or return "loaded" status immediately
  - `waitForAll()`, `waitForID()` return immediately
  - `checkAll()`, `checkID()` return `true`
  - Status methods return `COMPLETE`
- **Workaround**: Use browser's native image loading events or TImage
- **Implementation**: `TMediaTracker.java`

## UI Components

### `javax.swing.JFrame`
- **Status**: Working stub extending TFrame (TJFrame.java)
- **Reason**: Browser DOM requires different windowing approach
- **Behavior**: 
  - Extends `TFrame` for basic compatibility
  - `setDefaultCloseOperation()` has limited effect (no native close button)
  - Browser-based frames are rendered as DOM elements
- **Workaround**: Use `TFrame` directly or accept TJFrame's browser-specific behavior
- **Implementation**: `TJFrame.java`

### `java.awt.Cursor`
- **Status**: Stub implementation with constants (TCursor.java)
- **Reason**: Cursor changes typically handled via CSS in browsers
- **Behavior**: 
  - All cursor type constants defined
  - `getPredefinedCursor()`, `getDefaultCursor()` work
  - Actual cursor changes would need CSS integration
- **Workaround**: Use CSS cursor styles or accept stub behavior
- **Implementation**: `TCursor.java`

## Security and Cryptography

### `java.security.MessageDigest`
- **Status**: Stub implementation with limited algorithms (TMessageDigest.java)
- **Reason**: Limited crypto support in browser (Web Crypto API has different interface)
- **Behavior**: 
  - Supports SHA-1 and SHA-256 (stub implementations)
  - MD5 throws `NoSuchAlgorithmException`
  - **WARNING**: Current implementation returns dummy hashes (NOT SECURE)
  - Intended for compatibility only, not actual cryptographic use
- **Workaround**: Use Web Crypto API directly for real cryptographic operations
- **Implementation**: `TMessageDigest.java`
- **Security Note**: Do NOT rely on TMessageDigest for security-critical operations

## AWT Event Enhancements

### `java.awt.event.MouseWheelEvent.isControlDown()` / `isShiftDown()`
- **Status**: Fully implemented (TMouseWheelEvent.java)
- **Reason**: These methods were missing from the initial implementation
- **Behavior**: Check modifier flags from the event
- **Implementation**: Added to `TMouseWheelEvent.java`

## Container Methods

### `java.awt.Container.getSize()`
- **Status**: Fully implemented (TContainer.java)
- **Reason**: This method was missing from the initial implementation
- **Behavior**: Returns current size as TDimension
- **Implementation**: Added to `TContainer.java`

### `java.awt.Frame.dispose()`
- **Status**: Fully implemented (TFrame.java)
- **Reason**: This method was missing from the initial implementation
- **Behavior**: Hides the frame (browser resources are garbage collected)
- **Implementation**: Added to `TFrame.java`

## Image Color Models

### `java.awt.image.DirectColorModel` Extended Constructor
- **Status**: Fully implemented (TDirectColorModel.java)
- **Reason**: Constructor with ColorSpace parameter was missing
- **Behavior**: Constructs DirectColorModel with custom ColorSpace
- **Implementation**: Added constructor to `TDirectColorModel.java`

## Summary of Implementation Strategies

### 1. DetourHacks (Bytecode Transformation)
Used for `java.*` core classes that cannot be replaced:
- `java.io.File.toPath()` → `FileDetour.java`
- Registered in `META-INF/awtea.detours`

### 2. Stub Classes (Drop-in Replacements)
Used for missing classes that can be provided:
- AWT/Swing classes (TDesktop, TMediaTracker, TCursor, TJFrame)
- Utility classes (TImageIO, TMessageDigest)
- Located in `awtea-classlib` with `T` prefix

### 3. Method Additions
Used for missing methods in existing classes:
- `TMouseWheelEvent.isControlDown()`, `isShiftDown()`
- `TContainer.getSize()`
- `TFrame.dispose()`
- `TDirectColorModel` constructor

## Developer Guidance

When encountering browser-incompatible APIs:

1. **For `java.*` core classes**: Use DetourHacks to intercept calls
2. **For `javax.*` or `java.awt.*` classes**: Create `T`-prefixed stub in classlib
3. **For missing methods**: Add to existing `T`-prefixed class
4. **Always log warnings**: Help users understand browser limitations
5. **Document in this file**: Keep platform gaps documented

## Future Work

Potential improvements:
- Implement MessageDigest using Web Crypto API for real hashing
- Add more ImageIO support using canvas/blob APIs
- Enhance cursor support with CSS integration
- Add more NIO workarounds as needed

## References

- TeaVM limitations: https://teavm.org/
- Web Crypto API: https://developer.mozilla.org/en-US/docs/Web/API/Web_Crypto_API
- Browser security model: https://developer.mozilla.org/en-US/docs/Web/Security
