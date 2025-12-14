# Enum Code Generation System

This directory contains YAML schema files that define enums used across multiple languages (C, Java, and TypeScript) in the awtea project.

## Purpose

Previously, the same enums were manually defined in three different places:
- C headers (`awtea-graphics/src/main/native/`)
- Java code (`awtea-graphics/src/main/java/`)
- TypeScript tests (`awtea-graphics/src/test/deno/`)

This created maintenance overhead and risk of inconsistencies. The schema-based code generation system solves this by:
1. Defining each enum once in a YAML schema file
2. Automatically generating code for all three languages during the build
3. Ensuring consistency across all implementations

## Schema Files

### `surface-operation.yaml`
Defines operations that can be performed on a graphics surface (e.g., draw, fill, blit).

**Generated files:**
- C: `awtea-graphics/src/main/native/generated/surface_operation.h`
- Java: `awtea-graphics/src/main/java/me/mdbell/awtea/gfx/generated/Operation.java`
- TypeScript: `awtea-graphics/src/test/deno/generated/surface-operation.ts`

### `pixel-format.yaml`
Defines pixel format types for surfaces and images (e.g., ARGB, RGB, BGR).

**Generated files:**
- C: `awtea-graphics/src/main/native/generated/pixel_format.h`
- Java: `awtea-graphics/src/main/java/me/mdbell/awtea/gfx/generated/PixelFormat.java`
- TypeScript: `awtea-graphics/src/test/deno/generated/pixel-format.ts`

### `log-level.yaml`
Defines logging levels used by the unified logging system (ERROR, WARN, INFO, DEBUG, TRACE).

**Generated files:**
- C: `awtea-graphics/src/main/native/generated/log_level.h`
- Java: `awtea-graphics/src/main/java/me/mdbell/awtea/gfx/generated/LogLevel.java`
- TypeScript: `awtea-graphics/src/test/deno/generated/log-level.ts`

## Schema Format

Each YAML file defines an enum with the following structure:

```yaml
name: EnumName                    # Name of the enum (required)
description: Description text     # Description for documentation (optional)
c_prefix: PREFIX_                 # Prefix for C enum values (optional)
java_prefix: PREFIX_              # Prefix for Java constants (optional)
java_name: CustomName             # Custom name for Java class (optional)
java_package: com.example.pkg     # Custom package for Java (optional)
values:                           # List of enum values (required)
  - name: VALUE_NAME              # Value name (required)
    value: 0                      # Explicit numeric value (optional, auto-increments if omitted)
    description: Description      # Documentation comment (optional)
    c_only: false                 # Only generate for C (optional, default: false)
    java_only: false              # Only generate for Java (optional, default: false)
    ts_only: false                # Only generate for TypeScript (optional, default: false)
    java_skip: false              # Skip this value in Java (optional, default: false)
    c_name: CUSTOM_C_NAME         # Custom C name (optional)
    java_name: CUSTOM_JAVA_NAME   # Custom Java name (optional)
    ts_name: CUSTOM_TS_NAME       # Custom TypeScript name (optional)
```

## Adding or Modifying Enums

### To add a new enum:

1. Create a new YAML file in the `schemas/` directory following the format above
2. Run `./gradlew generateEnums` to generate code
3. Import and use the generated enum in your code

### To modify an existing enum:

1. Edit the corresponding YAML file in `schemas/`
2. Run `./gradlew generateEnums` to regenerate code
3. The changes will be reflected in all three languages

**Note:** The code generation task (`generateEnums`) runs automatically before compilation, so you typically don't need to run it manually.

## Language-Specific Options

### C Generation
- Generates `typedef enum` with the specified name
- Uses `c_prefix` to prefix all enum values
- Individual values can have custom names via `c_name`
- Values marked with `java_only` or `ts_only` are excluded

### Java Generation
- Generates either an `enum` (if no `java_prefix`) or an `interface` with `int` constants (if `java_prefix` is specified)
- Uses `java_prefix` to prefix constant names in interface mode
- Uses `java_name` for custom class/interface name (defaults to schema `name`)
- Uses `java_package` for custom package (defaults to `me.mdbell.awtea.gfx.generated`)
- Values marked with `c_only`, `ts_only`, or `java_skip` are excluded

### TypeScript Generation
- Generates `export enum` with numeric values
- Uses `c_prefix` for value names (to match C constants)
- Individual values can have custom names via `ts_name`
- Values marked with `c_only` or `java_only` are excluded

## Build Integration

The enum generation is integrated into the Gradle build system:

- **`./gradlew generateEnums`** - Manually generate all enums
- **`./gradlew cleanGeneratedEnums`** - Clean generated enum files
- **`./gradlew build`** - Automatically runs `generateEnums` before compilation

The generator is implemented in `buildSrc/src/main/java/me/mdbell/awtea/codegen/`.

## Generated File Headers

All generated files include a header comment:
```
/**
 * AUTO-GENERATED FILE - DO NOT EDIT
 * Generated from: schemas/[schema-name].yaml
 * 
 * [Description from schema]
 */
```

**DO NOT manually edit generated files.** Changes will be overwritten the next time code generation runs. Instead, edit the YAML schema files.

## Examples

### Example: Adding a new surface operation

Edit `schemas/surface-operation.yaml`:
```yaml
values:
  # ... existing values ...
  - name: DRAW_CIRCLE
    description: Draw a circle outline
```

Run generation:
```bash
./gradlew generateEnums
```

The new `CMD_DRAW_CIRCLE` (C), `DRAW_CIRCLE` (Java), and `CMD_DRAW_CIRCLE` (TypeScript) values are now available.

### Example: Platform-specific values

```yaml
values:
  - name: DEBUG
    value: 3
    description: Debug messages
  - name: TRACE
    value: 4
    description: Trace messages (available in all languages)
  - name: VERBOSE
    value: 5
    java_only: true
    description: Verbose messages (Java only)
```

This creates `VERBOSE` only in the Java enum, skipping C and TypeScript.
