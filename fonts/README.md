# Font Files for AWTea

This directory contains TrueType font files that are loaded at runtime by AWTea applications running in the browser.

## Purpose

Instead of bundling fonts as internal resources (which increases bundle size), AWTea loads fonts dynamically via HTTP fetch at runtime. This approach provides several benefits:

- **Reduced Bundle Size**: Font files are not included in the compiled JavaScript/WASM bundle
- **Browser Caching**: Fonts are cached by the browser's HTTP cache, improving performance on subsequent visits
- **Easy Updates**: Fonts can be updated without recompiling the application
- **CDN Support**: Fonts can be hosted on a CDN for better performance and distribution

## Deployment

When deploying an AWTea application, copy these font files to your web server or CDN so they are accessible at runtime.

### Default URL Structure

By default, AWTea expects fonts to be available at `fonts/{fontname}.ttf` relative to your application's base URL.

For example, if your application is at `https://example.com/app/`, fonts should be at:
- `https://example.com/app/fonts/NotoSans.ttf`
- `https://example.com/app/fonts/Helvetica.ttf`
- `https://example.com/app/fonts/Helvetica-Bold.ttf`
- `https://example.com/app/fonts/Helvetica-Italic.ttf`
- `https://example.com/app/fonts/Helvetica-BoldItalic.ttf`

### Custom Font URL

You can customize the base URL for fonts by calling:

```java
FontLoader.setFontBaseUrl("https://cdn.example.com/fonts/");
```

This should be called before any fonts are loaded (typically at application startup).

## Caching Strategy

### Browser HTTP Caching

Configure your web server to send appropriate cache-control headers for font files:

```
Cache-Control: public, max-age=31536000, immutable
```

This tells browsers to cache fonts for one year since font files rarely change.

### Cache Busting

If you need to update fonts and force clients to download new versions, you can:

1. **Version the URL**: Change the base URL to include a version number
   ```java
   FontLoader.setFontBaseUrl("fonts/v2/");
   ```

2. **Query Strings**: Append a version query parameter
   ```java
   FontLoader.setFontBaseUrl("fonts/?v=2");
   ```

3. **Hash in Filename**: Rename font files to include content hashes (requires code changes)

## Fallback Behavior

If fonts cannot be loaded via fetch (e.g., due to network errors or missing files), AWTea will attempt to load fonts from embedded resources as a fallback. However, for production deployments, you should ensure fonts are properly deployed and accessible.

## Font Files

This directory includes the following fonts:

- **Helvetica.ttf**: Standard Helvetica font
- **Helvetica-Bold.ttf**: Bold variant
- **Helvetica-Italic.ttf**: Italic variant
- **Helvetica-BoldItalic.ttf**: Bold italic variant
- **NotoSans.ttf**: Fallback font (Noto Sans)

## Testing Locally

When testing locally, you can serve these fonts using any HTTP server. For example:

```bash
# Using Python 3
cd /path/to/awtea
python3 -m http.server 8000

# Fonts will be available at http://localhost:8000/fonts/
```

## License

Please ensure you have appropriate licenses for any fonts you deploy. The fonts in this directory should be reviewed for licensing compliance before production use.
