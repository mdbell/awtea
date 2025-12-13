# Font Loading Strategy

AWTea uses a runtime font loading strategy that leverages browser HTTP caching to improve performance and reduce bundle size.

## Overview

Instead of embedding font files as internal resources in the compiled JavaScript/WebAssembly bundle, AWTea loads fonts dynamically at runtime via the Fetch API. This approach provides:

1. **Reduced Bundle Size**: Font files (typically 200-600 KB each) are not included in the bundle
2. **Browser HTTP Caching**: Fonts are cached by the browser, eliminating re-downloads on subsequent visits
3. **Easier Updates**: Fonts can be updated without recompiling or redeploying the application
4. **CDN Support**: Fonts can be hosted on CDNs for improved performance and global distribution

## Architecture

### Components

#### 1. FetchAPI (`awtea-util`)

A JSO (JavaScript Object) wrapper for the browser's Fetch API, providing type-safe access to:
- `fetch(url)`: Fetches a resource and returns a Promise<Response>
- `Response.arrayBuffer()`: Converts response body to an ArrayBuffer
- `Response.isOk()`: Checks if response was successful (200-299 status)

#### 2. FontLoader (`awtea-classlib`)

The main font loading utility that handles:
- Loading fonts via HTTP fetch in browser environments
- Caching loaded fonts in memory to avoid repeated network requests
- Falling back to embedded resources when fetch fails (for compatibility)
- Parsing TrueType font data and caching parsed font objects

Key methods:
- `FontLoader.loadFont(String fontName)`: Loads and parses a font
- `FontLoader.loadFontBytes(String fontName)`: Loads raw font data
- `FontLoader.getFontBaseUrl()`: Gets the configured base URL for fonts
- `FontLoader.clearCache()`: Clears the font cache

Key configuration:
- System property `me.mdbell.awtea.font.base_url`: Configures the base URL for fonts (default: "fonts/")

#### 3. TFont Updates

The `TFont` class has been updated to use `FontLoader` instead of directly loading from resources. The fallback to legacy resource loading is maintained for backward compatibility.

## Usage

### Basic Usage

By default, fonts are loaded from `fonts/{fontname}.ttf` relative to your application's base URL:

```java
// No configuration needed - uses default URL
TFont font = new TFont("Helvetica", TFont.BOLD, 12);
```

### Custom Font URL

To load fonts from a custom location (e.g., CDN), set the system property:

```bash
# Set via command line
java -Dme.mdbell.awtea.font.base_url=https://cdn.example.com/myfonts/ -jar myapp.jar
```

Fonts will now be loaded from:
- https://cdn.example.com/myfonts/Helvetica.ttf
- https://cdn.example.com/myfonts/Helvetica-Bold.ttf
- etc.

### Cache Management

Clear the font cache if needed (e.g., after loading new font versions):

```java
FontLoader.clearCache();
```

## Deployment

### 1. Copy Font Files

Copy the font files from the `fonts/` directory to your web server or CDN:

```bash
# Example: Deploy to web server
scp fonts/*.ttf user@webserver:/var/www/html/myapp/fonts/

# Example: Upload to CDN
aws s3 cp fonts/ s3://my-bucket/fonts/ --recursive
```

### 2. Configure Web Server

Set appropriate cache headers for font files:

**Nginx:**
```nginx
location /fonts/ {
    add_header Cache-Control "public, max-age=31536000, immutable";
    add_header Access-Control-Allow-Origin "*";
}
```

**Apache:**
```apache
<Directory "/var/www/html/fonts">
    Header set Cache-Control "public, max-age=31536000, immutable"
    Header set Access-Control-Allow-Origin "*"
</Directory>
```

**Note**: CORS headers are important if fonts are hosted on a different domain than your application.

### 3. Configure Font URL (Optional)

If fonts are not in the default `fonts/` directory, set the system property:

```bash
# Via command line
java -Dme.mdbell.awtea.font.base_url=https://cdn.example.com/fonts/v1/ -jar myapp.jar

# Or via environment/config
export JAVA_OPTS="-Dme.mdbell.awtea.font.base_url=https://cdn.example.com/fonts/v1/"
```

## Caching Strategy

### Browser HTTP Caching

AWTea relies on standard HTTP caching mechanisms:

1. **Initial Load**: Font is fetched from the server and cached by the browser
2. **Subsequent Loads**: Browser serves font from its HTTP cache (no network request)
3. **Cache Duration**: Controlled by `Cache-Control` headers from your server

Recommended cache settings:
- `max-age=31536000` (1 year) for production fonts that don't change
- `immutable` directive to indicate the file will never change at this URL

### In-Memory Caching

FontLoader maintains two levels of in-memory caching:

1. **Byte Cache**: Raw font data (byte arrays) are cached after fetching
2. **Parsed Font Cache**: Parsed TrueTypeFont objects are cached to avoid re-parsing

Both caches persist for the lifetime of the application, eliminating redundant network requests and parsing operations.

### Cache Invalidation

To force clients to download new font versions:

#### Option 1: Version in URL
```bash
-Dme.mdbell.awtea.font.base_url=fonts/v2/  # Change version number
```

#### Option 2: Query Parameter
```bash
-Dme.mdbell.awtea.font.base_url=fonts/?v=1.2.3
```

#### Option 3: Hash in Filename
Rename font files to include content hashes:
- `Helvetica.abc123def.ttf`
- Requires updating font name mappings in your code

## Fallback Behavior

### Non-Browser Environments

In JVM/test environments where the Fetch API is not available, FontLoader automatically falls back to loading fonts from embedded resources (`/fonts/{fontname}.ttf`).

### Network Failures

If a font fetch fails in the browser (e.g., 404, network error), FontLoader attempts to load from embedded resources as a fallback. This provides resilience but increases bundle size if fallback fonts are included.

**Recommendation**: Ensure fonts are properly deployed to avoid fallback behavior in production.

## Testing

### Local Testing

Serve fonts locally during development:

```bash
# From the awtea root directory
python3 -m http.server 8000

# Fonts available at http://localhost:8000/fonts/
```

Configure your application to use the local URL:
```bash
java -Dme.mdbell.awtea.font.base_url=http://localhost:8000/fonts/ -jar myapp.jar
```

### Verifying Cache Behavior

1. Open browser DevTools (F12)
2. Go to Network tab
3. Load your application
4. Check for font requests:
   - First load: Should see HTTP 200 responses
   - Subsequent loads: Should see "(from disk cache)" or HTTP 304

## Performance Considerations

### Bundle Size Reduction

Removing fonts from resources saves approximately:
- Helvetica: ~318 KB
- Helvetica-Bold: ~309 KB
- Helvetica-Italic: ~598 KB
- Helvetica-BoldItalic: ~277 KB
- NotoSans: ~622 KB

**Total savings**: ~2.1 MB (uncompressed)

### Loading Performance

- **Initial Load**: Slightly slower (fetches fonts over network)
- **Subsequent Loads**: Much faster (served from browser cache)
- **Cold Cache**: Similar to embedded resources
- **Warm Cache**: Near-instant font availability

### Best Practices

1. **Preload Critical Fonts**: Use `<link rel="preload">` in HTML:
   ```html
   <link rel="preload" href="fonts/Helvetica.ttf" as="font" type="font/ttf" crossorigin>
   ```

2. **Use CDN**: Host fonts on a CDN for better global performance

3. **Minimize Font Files**: Only deploy fonts your application actually uses

4. **Compress**: Ensure web server sends fonts with gzip/brotli compression

## Security Considerations

### CORS

If fonts are hosted on a different domain, ensure CORS headers are set:
```
Access-Control-Allow-Origin: *
```

Or more restrictively:
```
Access-Control-Allow-Origin: https://myapp.example.com
```

### Content Security Policy

If using CSP, ensure font sources are allowed:
```
Content-Security-Policy: font-src 'self' https://cdn.example.com;
```

### Subresource Integrity

For additional security, you can verify font integrity (though less common for fonts):
```html
<link rel="preload" href="fonts/Helvetica.ttf" as="font" 
      integrity="sha384-oqVuAfXRKap7fdgcCY5uykM6+R9GqQ8K/uxy9rx7HNQlGYl1kPzQho1wx4JwY8wC"
      crossorigin>
```

## Migration Guide

If you have existing AWTea applications that rely on embedded fonts:

1. **Update Dependencies**: Pull the latest awtea-classlib and awtea-util
2. **Deploy Fonts**: Copy font files to your web server
3. **Configure URL** (if not using default): Set system property `-Dme.mdbell.awtea.font.base_url=<url>`
4. **Test**: Verify fonts load correctly
5. **Remove Legacy Resources** (optional): If no longer needed for fallback

The change is backward compatible - existing code continues to work without modifications.

## Troubleshooting

### Fonts Not Loading

1. **Check Network Tab**: Look for 404 or other errors on font requests
2. **Verify URL**: Ensure `FontLoader.getFontBaseUrl()` returns the correct path
3. **Check CORS**: If cross-origin, verify CORS headers are set
4. **Test Direct Access**: Try accessing font URL directly in browser

### Fallback to Resources

If you see messages like "Fetch failed for font X, trying resource fallback":
- Font file is not accessible at the configured URL
- Network connectivity issues
- CORS issues

### Cache Not Working

- Verify `Cache-Control` headers are sent by your server
- Check browser cache settings (some browsers have cache disabled in DevTools)
- Try in a regular browser window (not private/incognito mode)

## Future Enhancements

Potential improvements for the font loading system:

- **WOFF2 Support**: Load more efficient WOFF2 format fonts
- **Progressive Loading**: Load font subsets on demand
- **Font Face API**: Use CSS Font Loading API for better integration
- **Service Worker**: Cache fonts via Service Worker for offline support
- **Preconnect**: Add `<link rel="preconnect">` for CDN domains
