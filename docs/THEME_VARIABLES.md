# Theme CSS Variables Reference

This document describes all CSS custom properties (CSS variables) used in the awtea theming system. These variables provide a consistent way to style UI components and support both light and dark themes.

## Overview

The awtea theme system uses CSS custom properties prefixed with `--aw-` to enable runtime theming and consistent styling across all UI components. Variables are defined in the `Theme.java` class and automatically switch between light and dark mode based on user preference.

## Usage

### In CSS Files

Reference theme variables using the standard CSS `var()` function:

```css
.my-component {
  background: var(--aw-bg);
  color: var(--aw-fg);
  border: 1px solid var(--aw-border);
}
```

### In Java Code

Access theme variables through the `Theme.Var` enum:

```java
// Using with AwCss
sheet()
  .rule(".my-class")
  .prop("background", Theme.Var.BACKGROUND)
  .prop("color", Theme.Var.FOREGROUND)
  .end();

// Setting runtime values
element.getStyle().setProperty(
  Theme.Var.BACKGROUND.getCssVarName(), 
  "#ffffff"
);
```

## Core Theme Variables

### General Colors

| Variable | CSS Name | Light Mode | Dark Mode | Usage |
|----------|----------|------------|-----------|-------|
| `BACKGROUND` | `--aw-bg` | `rgba(255,255,255,0.97)` | `#1e1e1e` | Primary background color for windows and panels |
| `FOREGROUND` | `--aw-fg` | `#000` | `#eee` | Primary text and foreground color |
| `BORDER` | `--aw-border` | `#ccc` | `#444` | Border color for components and dividers |
| `SHADOW` | `--aw-shadow` | `rgba(0,0,0,0.25)` | `rgba(0,0,0,0.6)` | Drop shadow color for elevated elements |
| `ACCENT` | `--aw-accent` | `#0066cc` | `#3399ff` | Accent color for interactive elements and highlights |

### Scrollbar Colors

| Variable | CSS Name | Light Mode | Dark Mode | Usage |
|----------|----------|------------|-----------|-------|
| `SCROLLBAR_THUMB` | `--aw-scrollbar-thumb` | `rgba(0,0,0,0.2)` | `rgba(255,255,255,0.2)` | Scrollbar thumb (draggable part) |
| `SCROLLBAR_TRACK` | `--aw-scrollbar-track` | `rgba(0,0,0,0.1)` | `rgba(255,255,255,0.1)` | Scrollbar track (background) |

### Header Colors

| Variable | CSS Name | Light Mode | Dark Mode | Usage |
|----------|----------|------------|-----------|-------|
| `HEADER_BACKGROUND` | `--aw-header-bg` | `#f0f0f0` | `#2b2b2b` | Background for window headers, menu bars, and section headers |
| `HEADER_BORDER` | `--aw-header-border` | `#ddd` | `#444` | Border color for headers |

### Entry & Metadata Colors

| Variable | CSS Name | Light Mode | Dark Mode | Usage |
|----------|----------|------------|-----------|-------|
| `ENTRY_BORDER` | `--aw-entry-border` | `#f0f0f0` | `#333` | Border color for individual entries in lists |
| `META_FOREGROUND` | `--aw-meta-fg` | `#777` | `#aaa` | Color for metadata text (timestamps, descriptions) |
| `TYPE_FOREGROUND` | `--aw-type-fg` | `#666` | `#aaa` | Color for type information and labels |

### Button Colors

| Variable | CSS Name | Light Mode | Dark Mode | Usage |
|----------|----------|------------|-----------|-------|
| `BUTTON_BACKGROUND` | `--aw-button-bg` | `#f5f5f5` | `#333` | Background color for buttons |
| `BUTTON_BORDER` | `--aw-button-border` | `#888` | `#777` | Border color for buttons |

## Log Level Colors

Used in the LogFrame component for different severity levels:

| Variable | CSS Name | Light Mode | Dark Mode | Usage |
|----------|----------|------------|-----------|-------|
| `ERROR_FOREGROUND` | `--aw-error-fg` | `#b00020` | `#ff8080` | Error messages and critical warnings |
| `WARNING_FOREGROUND` | `--aw-warning-fg` | `#e65c00` | `#ffb366` | Warning messages |
| `INFO_FOREGROUND` | `--aw-info-fg` | `#0066cc` | `#3399ff` | Informational messages |
| `DEBUG_FOREGROUND` | `--aw-debug-fg` | `#444` | `#ccc` | Debug messages and verbose output |

## Table Colors

Used in monitor frames and data tables:

| Variable | CSS Name | Light Mode | Dark Mode | Usage |
|----------|----------|------------|-----------|-------|
| `TABLE_HEADER_BACKGROUND` | `--aw-table-header-bg` | `#f5f5f5` | `#2b2b2b` | Background for table headers |
| `TABLE_HEADER_BORDER` | `--aw-table-header-border` | `#dddddd` | `#444444` | Border color for table headers |
| `TABLE_ROW_BACKGROUND` | `--aw-table-row-bg` | `#ffffff` | `#242424` | Background for odd table rows |
| `TABLE_ROW_ALT_BACKGROUND` | `--aw-table-row-alt-bg` | `#f9f9f9` | `#1e1e1e` | Background for even table rows (striping) |
| `TABLE_ROW_HOVER_BACKGROUND` | `--aw-table-row-hover-bg` | `#e6f2ff` | `#303846` | Background when hovering over a table row |
| `TABLE_ROW_BORDER` | `--aw-table-row-border` | `#eeeeee` | `#333333` | Border color between table rows |

## Status Row Colors

Used to indicate status in table rows or list items:

| Variable | CSS Name | Light Mode | Dark Mode | Usage |
|----------|----------|------------|-----------|-------|
| `ROW_STATUS_MUTED_BACKGROUND` | `--aw-row-status-muted-bg` | `rgba(160,160,160,0.18)` | `rgba(140,140,140,0.25)` | Muted/inactive state (grey) |
| `ROW_STATUS_OK_BACKGROUND` | `--aw-row-status-ok-bg` | `rgba(0,160,0,0.18)` | `rgba(0,200,0,0.22)` | Success/healthy state (green) |
| `ROW_STATUS_WARN_BACKGROUND` | `--aw-row-status-warn-bg` | `rgba(230,190,0,0.2)` | `rgba(230,210,80,0.24)` | Warning state (amber) |
| `ROW_STATUS_ERROR_BACKGROUND` | `--aw-row-status-error-bg` | `rgba(220,60,60,0.24)` | `rgba(230,80,80,0.26)` | Error/critical state (red) |

## Meter/Progress Colors

Used for progress bars, meters, and performance indicators:

| Variable | CSS Name | Light Mode | Dark Mode | Usage |
|----------|----------|------------|-----------|-------|
| `METER_BACKGROUND` | `--aw-meter-bg` | `#dddddd` | `#333333` | Background of meter/progress bar |
| `METER_GOOD` | `--aw-meter-good` | `#4caf50` | `#66bb6a` | Good/optimal performance (green) |
| `METER_WARN` | `--aw-meter-warn` | `#ffb300` | `#ffca28` | Warning/elevated performance (amber) |
| `METER_BAD` | `--aw-meter-bad` | `#f44336` | `#ef5350` | Bad/critical performance (red) |

## Theme Switching

The theme automatically switches between light and dark mode based on:

1. **User preference**: Stored in browser local storage (`awtea.dark-mode`)
2. **System preference**: Detected via `prefers-color-scheme` media query
3. **Manual toggle**: Can be toggled programmatically via `Theme.setDarkMode(boolean)`

Dark mode is applied by adding the `.aw-dark-mode` class to the document body, which overrides the `:root` CSS variable values.

## Best Practices

### 1. Always Use Theme Variables

**Good:**
```css
.my-component {
  background: var(--aw-bg);
  color: var(--aw-fg);
}
```

**Avoid:**
```css
.my-component {
  background: #ffffff;
  color: #000000;
}
```

### 2. Provide Fallback Values for Custom Components

When extending the theme with component-specific variables, provide fallbacks:

```css
.custom-component {
  background: var(--custom-bg, var(--aw-bg));
  border-color: var(--custom-border, var(--aw-border));
}
```

### 3. Use Semantic Color Names

Choose the theme variable that best represents the semantic purpose:

- Use `--aw-error-fg` for errors, not `--aw-meter-bad`
- Use `--aw-header-bg` for headers, not generic `--aw-bg`
- Use `--aw-table-row-hover-bg` for hover states in tables

### 4. Maintain Contrast

When customizing theme values, ensure sufficient contrast for accessibility:

- Background/foreground combinations should meet WCAG AA standards (4.5:1 ratio)
- Border colors should be distinguishable from backgrounds

## Related Documentation

- [CSS Embedding Guide](CSS_EMBEDDING.md) - How to embed CSS files using `@CSSSource`
- [Component Mapping](COMPONENT_MAPPING.md) - How AWT components map to web technologies

## Source Code

The theme system is implemented in:
- **Java**: [`awtea-ui/src/main/java/me/mdbell/awtea/ui/Theme.java`](../awtea-ui/src/main/java/me/mdbell/awtea/ui/Theme.java)
- **CSS Variables**: Programmatically generated and injected at runtime
