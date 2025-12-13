# CSS Embedding in awtea

## Overview

awtea provides a way to embed CSS files at compile time using TeaVM's code generation capabilities. This allows you to package CSS styles directly into your application and inject them at runtime with support for CSS custom properties for component-level theming.

## How It Works

The CSS embedding system uses the same infrastructure as shader embedding:

1. **`@CSSSource` annotation**: Mark native methods to embed CSS files from classpath
2. **`EmbedGenerator`**: TeaVM generator that loads and embeds the CSS content at compile time
3. **CSS Custom Properties**: Use standard CSS variables (`var(--name)`) for runtime customization

## Basic Usage

### 1. Create a CSS File

Create a CSS file in your `src/main/resources` directory:

**`src/main/resources/styles/component.css`:**
```css
.mycomponent {
  background: var(--bg-color);
  color: var(--text-color);
  border: 1px solid var(--border-color);
  padding: var(--padding, 8px);
}

.mycomponent-header {
  background: var(--header-bg-color);
  color: var(--header-text-color);
  font-weight: bold;
}
```

### 2. Create a Utility Class to Load the CSS

**`Styles.java`:**
```java
package me.mdbell.awtea.gl;

import lombok.experimental.UtilityClass;
import org.teavm.backend.javascript.spi.GeneratedBy;

@UtilityClass
public class Styles {

    @CSSSource("styles/component.css")
    @GeneratedBy(EmbedGenerator.class)
    public native String componentCSS();
}
```

### 3. Inject the CSS at Runtime

```java
import me.mdbell.awtea.ui.AwCss;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

// Get the embedded CSS
String css = Styles.componentCSS();

// Create a <style> element and inject it
HTMLElement style = Window.current()
    .getDocument()
    .createElement("style");
style.setTextContent(css);
Window.current()
    .getDocument()
    .getHead()
    .appendChild(style);
```

## Using CSS Custom Properties for Theming

CSS custom properties (CSS variables) allow you to customize styles at runtime without modifying the CSS itself.

### Defining Variables in CSS

Use the standard `var()` notation with fallback values:

```css
.widget {
  /* Required variable */
  background: var(--widget-bg);
  
  /* Variable with fallback */
  padding: var(--widget-padding, 12px);
  
  /* Nested variables */
  border: 1px solid var(--widget-border-color, var(--default-border));
}
```

### Setting Variables at Runtime

You can set CSS custom properties using JavaScript interop or the `AwCss` utility:

**Using JavaScript interop directly:**
```java
import org.teavm.jso.dom.html.HTMLElement;

HTMLElement element = ...; // Your component's element
element.getStyle().setProperty("--bg-color", "#ffffff");
element.getStyle().setProperty("--text-color", "#000000");
```

**Using the AwCss Sheet API:**
```java
import static me.mdbell.awtea.ui.AwCss.*;

// Define variables in :root for global theming
sheet()
    .rule(":root")
    .prop("--bg-color", "#ffffff")
    .prop("--text-color", "#000000")
    .prop("--border-color", "#cccccc")
    .end()
    .inject();

// Or scope to specific classes
sheet()
    .createClass("mycomponent")
    .prop("--bg-color", "#f0f0f0")
    .prop("--text-color", "#333333")
    .end()
    .inject();
```

## Integration with Existing Theme System

The awtea project already uses CSS custom properties extensively in the `Theme` class for dark mode support. You can leverage existing theme variables or define your own:

**Using existing theme variables:**
```css
.custom-panel {
  background: var(--aw-bg);
  color: var(--aw-fg);
  border: 1px solid var(--aw-border);
}
```

See [`Theme.java`](../awtea-ui/src/main/java/me/mdbell/awtea/ui/Theme.java) for all available theme variables.

## Example: Complete Component Styling

**1. Create CSS file (`styles/panel.css`):**
```css
.custom-panel {
  background: var(--panel-bg, #ffffff);
  color: var(--panel-fg, #000000);
  border: 1px solid var(--panel-border, #cccccc);
  border-radius: var(--panel-radius, 4px);
  padding: var(--panel-padding, 16px);
  box-shadow: 0 2px 4px var(--panel-shadow, rgba(0,0,0,0.1));
}

.custom-panel-title {
  font-size: var(--panel-title-size, 18px);
  font-weight: bold;
  margin-bottom: var(--panel-title-margin, 12px);
  color: var(--panel-title-color, var(--panel-fg));
}
```

**2. Load and inject:**
```java
@UtilityClass
public class CustomStyles {
    @CSSSource("styles/panel.css")
    @GeneratedBy(EmbedGenerator.class)
    public native String panelCSS();
}

// In your initialization code:
public class MyApp {
    public void init() {
        HTMLElement style = Window.current()
            .getDocument()
            .createElement("style");
        style.setTextContent(CustomStyles.panelCSS());
        Window.current()
            .getDocument()
            .getHead()
            .appendChild(style);
    }
}
```

**3. Apply to components:**
```java
HTMLElement panel = Window.current().getDocument().createElement("div");
panel.getClassList().add("custom-panel");

// Optionally customize with CSS variables
panel.getStyle().setProperty("--panel-bg", "#f0f8ff");
panel.getStyle().setProperty("--panel-border", "#4a90e2");
```

## Benefits

- **Compile-time validation**: CSS files are validated at compile time - missing files cause build failures
- **Zero runtime overhead**: CSS is embedded directly into the JavaScript output
- **Type-safe access**: CSS is accessed via Java methods with compile-time checking
- **Component-level theming**: CSS custom properties enable flexible runtime customization
- **Standards-based**: Uses standard CSS features - no custom templating language
- **Reusable infrastructure**: Shares the same generator code as shader embedding

## Comparison with Other Approaches

### vs. Inline CSS Strings
```java
// ❌ Inline strings - error-prone, no syntax highlighting
String css = ".panel { background: red; color: blue; }";

// ✅ Embedded CSS - syntax checked by your IDE, validated at compile time
String css = Styles.panelCSS();
```

### vs. External CSS Files
```java
// ❌ External files - requires separate HTTP request, not bundled
HTMLElement link = document.createElement("link");
link.setAttribute("rel", "stylesheet");
link.setAttribute("href", "styles.css");

// ✅ Embedded CSS - bundled with application, no extra requests
HTMLElement style = document.createElement("style");
style.setTextContent(Styles.componentCSS());
```

### vs. Programmatic Styling
```java
// ❌ Programmatic - verbose, less maintainable
element.getStyle().setProperty("background-color", "#ffffff");
element.getStyle().setProperty("color", "#000000");
element.getStyle().setProperty("border", "1px solid #cccccc");

// ✅ CSS with variables - declarative, maintainable
// Define once in CSS, customize at runtime with variables
element.getStyle().setProperty("--bg-color", "#ffffff");
```

## Technical Details

### Annotation Definition

The `@CSSSource` annotation is defined similarly to `@ShaderSource`:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CSSSource {
    /**
     * Classpath resource path to the CSS file.
     */
    String value();
}
```

### Generator Implementation

The `EmbedGenerator` supports both `@ShaderSource` and `@CSSSource`:

1. Checks for either annotation on the annotated method
2. Loads the resource from the classpath using the provided path
3. Escapes the content for JavaScript string literals
4. Generates code that returns the embedded string at runtime

## Best Practices

1. **Use CSS custom properties for theming**: Define variables for all customizable values
2. **Provide fallback values**: Use `var(--name, fallback)` for graceful degradation
3. **Organize by component**: Create separate CSS files for different components
4. **Leverage existing theme**: Reuse variables from `Theme.Var` when possible
5. **Inject once**: Cache and reuse the injected style element instead of re-injecting
6. **Scope your styles**: Use specific class names to avoid conflicts

## Related Files

- [`CSSSource.java`](../awtea-graphics/src/main/java/me/mdbell/awtea/gl/CSSSource.java) - Annotation definition
- [`EmbedGenerator.java`](../awtea-graphics/src/main/java/me/mdbell/awtea/gl/EmbedGenerator.java) - TeaVM generator
- [`Styles.java`](../awtea-graphics/src/main/java/me/mdbell/awtea/gl/Styles.java) - Example usage
- [`Theme.java`](../awtea-ui/src/main/java/me/mdbell/awtea/ui/Theme.java) - Theme system with CSS variables
- [`AwCss.java`](../awtea-ui/src/main/java/me/mdbell/awtea/ui/AwCss.java) - CSS generation utility
