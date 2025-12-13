# Font Loading Examples

This document provides examples of how to use the new runtime font loading system in AWTea.

## Basic Usage (Default Configuration)

The simplest case - no configuration needed. Fonts are loaded from the default `fonts/` directory:

```java
import me.mdbell.awtea.classlib.java.awt.TFont;
import me.mdbell.awtea.classlib.java.applet.TApplet;

public class BasicFontExample extends TApplet {
    @Override
    public void init() {
        // Fonts are automatically loaded from fonts/ directory
        TFont plainFont = new TFont("Helvetica", TFont.PLAIN, 12);
        TFont boldFont = new TFont("Helvetica", TFont.BOLD, 14);
        TFont italicFont = new TFont("Helvetica", TFont.ITALIC, 12);
        
        // Use fonts as usual...
    }
}
```

## Custom Font URL

Configure fonts to load from a CDN or custom location using the system property:

```java
import me.mdbell.awtea.classlib.java.awt.TFont;
import me.mdbell.awtea.classlib.java.applet.TApplet;

public class CdnFontExample extends TApplet {
    @Override
    public void init() {
        // Fonts will be loaded from the configured URL
        // Set via: -Dme.mdbell.awtea.font.base_url=https://cdn.example.com/fonts/
        TFont font = new TFont("Helvetica", TFont.PLAIN, 12);
        
        // Fonts are cached automatically - subsequent uses don't hit the network
        TFont sameFontAgain = new TFont("Helvetica", TFont.PLAIN, 12);
    }
}
```

Set the system property when starting your application:

```bash
java -Dme.mdbell.awtea.font.base_url=https://cdn.example.com/fonts/ -jar myapp.jar
```

## Relative Path

Loading fonts from a different relative path:

```bash
# Set via system property
java -Dme.mdbell.awtea.font.base_url=assets/typography/ -jar myapp.jar
```

## Versioned Fonts (Cache Busting)

Force browsers to load new font versions using system properties:

```bash
# Use query parameter for versioning
java -Dme.mdbell.awtea.font.base_url=fonts/?version=v1.2.0 -jar myapp.jar

# Or use path versioning
java -Dme.mdbell.awtea.font.base_url=fonts/v1.2.0/ -jar myapp.jar
```

## Cache Management

Clearing the font cache when needed:

```java
import me.mdbell.awtea.font.FontLoader;
import me.mdbell.awtea.classlib.java.awt.TFont;

public class CacheManagementExample {
    public void reloadFonts() {
        TFont font1 = new TFont("Helvetica", TFont.PLAIN, 12);
        
        // Font is now cached in memory
        System.out.println("Font loaded and cached");
        
        // Clear cache if needed (e.g., before loading different version)
        FontLoader.clearCache();
        
        // Note: To change the font URL, you need to restart with a different
        // system property: -Dme.mdbell.awtea.font.base_url=fonts/v2/
        
        // This will reload from the configured URL
        TFont font2 = new TFont("Helvetica", TFont.PLAIN, 12);
    }
}
```

## Loading Custom Fonts

Loading fonts via createFont (works the same as before):

```java
import me.mdbell.awtea.classlib.java.awt.TFont;
import me.mdbell.awtea.classlib.java.awt.image.TFontFormatException;
import me.mdbell.awtea.classlib.java.applet.TApplet;
import java.io.IOException;
import java.io.InputStream;

public class CustomFontExample extends TApplet {
    @Override
    public void init() {
        try {
            // Load a custom font from a stream
            InputStream fontStream = getClass().getResourceAsStream("/custom/MyFont.ttf");
            TFont customFont = TFont.createFont(TFont.TRUETYPE_FONT, fontStream);
            
            // Derive to desired size and style
            TFont sized = customFont.deriveFont(14f);
            TFont bold = customFont.deriveFont(TFont.BOLD, 16f);
            
            // Use the fonts...
        } catch (IOException | TFontFormatException e) {
            System.err.println("Failed to load custom font: " + e);
        }
    }
}
```

## Complete Application Example

A complete example showing font configuration and usage:

```java
import me.mdbell.awtea.classlib.java.awt.*;
import me.mdbell.awtea.classlib.java.applet.TApplet;

public class FontDemoApp extends TApplet {
    private TFont titleFont;
    private TFont bodyFont;
    private TFont codeFont;
    
    @Override
    public void init() {
        // Configure font loading at startup
        setupFonts();
        
        // Create UI with different fonts
        setSize(800, 600);
        setLayout(new BorderLayout());
        
        // Create components using the loaded fonts
        TLabel title = new TLabel("Welcome to AWTea", TLabel.CENTER);
        title.setFont(titleFont);
        add(title, BorderLayout.NORTH);
        
        TTextArea body = new TTextArea("Body text goes here...");
        body.setFont(bodyFont);
        add(body, BorderLayout.CENTER);
        
        TTextArea code = new TTextArea("System.out.println(\"Hello\");");
        code.setFont(codeFont);
        add(code, BorderLayout.SOUTH);
    }
    
    private void setupFonts() {
        // Fonts are configured via system property:
        // -Dme.mdbell.awtea.font.base_url=https://fonts.example.com/awtea/
        // 
        // Or use default location (fonts/) if not configured
        
        // Create fonts
        titleFont = new TFont("Helvetica", TFont.BOLD, 24);
        bodyFont = new TFont("Helvetica", TFont.PLAIN, 12);
        codeFont = new TFont("NotoSans", TFont.PLAIN, 11);
    }
    
    @Override
    public void start() {
        System.out.println("Font loading example started");
        System.out.println("Fonts loaded from: " + FontLoader.getFontBaseUrl());
    }
}
```

## Deployment Checklist

When deploying an application using the new font loading system:

1. **Copy font files** from `fonts/` directory to your web server
2. **Configure web server** to send proper cache headers:
   ```
   Cache-Control: public, max-age=31536000, immutable
   Access-Control-Allow-Origin: *
   ```
3. **Set font base URL** via system property (if not using default `fonts/`):
   ```bash
   -Dme.mdbell.awtea.font.base_url=https://cdn.example.com/fonts/
   ```
4. **Test font loading** using browser DevTools Network tab
5. **Verify caching** by checking for "(from cache)" on subsequent loads

## Troubleshooting

### Fonts not loading?

Check the browser console and network tab for errors:

- 404: Font file not found at the URL
- CORS error: Add `Access-Control-Allow-Origin` header
- Network error: Check URL configuration

### Verify configuration:

```java
System.out.println("Font base URL: " + FontLoader.getFontBaseUrl());
```

### Clear cache for testing:

```java
FontLoader.clearCache();
```

## Performance Tips

1. **Preload fonts** in HTML:
   ```html
   <link rel="preload" href="fonts/Helvetica.ttf" as="font" type="font/ttf" crossorigin>
   ```

2. **Use HTTP/2** for parallel font loading

3. **Enable compression** on your web server (gzip/brotli)

4. **Use a CDN** for better global performance

5. **Minimize fonts**: Only deploy fonts you actually use

## See Also

- [Font Loading Strategy Documentation](FONT_LOADING.md)
- [Font Deployment Guide](../webapp-common/fonts/README.md)
