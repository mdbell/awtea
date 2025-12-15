# Field Accessor System for Method Detours

## Overview

This module provides a compile-time code generation system that allows detour classes to access private fields of target classes without reflection. This is achieved through TeaVM's Generator API.

## Components

### Annotations

- **`@FieldAccessor`**: Marks a native method as a field getter
- **`@FieldSetter`**: Marks a native method as a field setter
- **`@DetourReceiver`**: Identifies the target class (existing)
- **`@DetourMethod`**: Marks methods to detour (existing)

### Generator

- **`FieldAccessorGenerator`**: TeaVM Generator that produces JavaScript code for field access at compile-time

### Example Detour

See `detour/FieldAccessorExample.java` for a complete working example.

## How It Works

### 1. Declare Field Accessors

In your detour class, declare native methods annotated with `@FieldAccessor` or `@FieldSetter`:

```java
@DetourReceiver(target = MyTarget.class)
public class MyTargetDetour {
    
    @FieldAccessor("privateField")
    @GeneratedBy(FieldAccessorGenerator.class)
    private static native int getPrivateField(MyTarget instance);
    
    @FieldSetter("privateField")
    @GeneratedBy(FieldAccessorGenerator.class)
    private static native void setPrivateField(MyTarget instance, int value);
}
```

### 2. TeaVM Compilation

During TeaVM compilation, the `FieldAccessorGenerator` is invoked for each native method annotated with `@GeneratedBy(FieldAccessorGenerator.class)`:

1. Generator reads the `@FieldAccessor` or `@FieldSetter` annotation
2. Validates method signature (parameter count, return type)
3. Generates JavaScript code for field access
4. Injects generated code into the compiled JavaScript

### 3. Generated JavaScript

For a field accessor:
```javascript
// Java: getPrivateField(instance)
return instance.privateField;
```

For a field setter:
```javascript
// Java: setPrivateField(instance, value)
instance.privateField = value;
```

### 4. Use in Detour Methods

The generated accessors can be called from detour methods:

```java
@DetourMethod("someMethod")
public static void someMethod(MyTarget self, int arg) {
    int current = getPrivateField(self);
    setPrivateField(self, current + arg);
}
```

## Registration

Detour classes are registered in `META-INF/awtea.detours`:

```
# System-level detours
me.mdbell.awtea.instrument.detour.SystemDetour
me.mdbell.awtea.instrument.detour.ThreadDetour

# Your custom detours
com.example.MyTargetDetour
```

## Use Cases

### Zero-Copy WASM Rendering

Access WASM-native data structures directly:

```java
@DetourMethod("create")
public static void create(BufferedPixMap self, int width, int height) {
    // Create image with WASM-native pixel buffer
    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    int[] wasmData = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();
    
    // Set private field directly (no copy!)
    setData(self, wasmData);
}
```

### Performance Optimization

Bypass reflection overhead in hot code paths:

```java
@DetourMethod("render")
public static void render(Renderer self) {
    // Direct field access - no Method.invoke()
    int[] pixels = getPixelBuffer(self);
    int width = getWidth(self);
    
    // Fast rendering loop
    for (int i = 0; i < pixels.length; i++) {
        pixels[i] = computeColor(i, width);
    }
}
```

## API Requirements

### Field Accessor Methods

Must satisfy:
- `private static native` modifiers
- Exactly one parameter (target instance)
- Non-void return type matching field type
- Declared in `@DetourReceiver` class

### Field Setter Methods

Must satisfy:
- `private static native` modifiers
- Exactly two parameters (target instance, new value)
- `void` return type
- Second parameter type matches field type
- Declared in `@DetourReceiver` class

## Error Messages

| Error | Cause | Fix |
|-------|-------|-----|
| "must have exactly one parameter" | Wrong parameter count for getter | Add exactly one parameter |
| "must have exactly two parameters" | Wrong parameter count for setter | Add exactly two parameters |
| "must return void" | Setter has non-void return | Change return type to void |
| "must return the field value, not void" | Getter returns void | Change return type to field type |
| "declaring class must be annotated with @DetourReceiver" | Missing @DetourReceiver | Add annotation to class |

## See Also

- `DetourHacks.java` - Method detour implementation
- `CustomTransformersPlugin.java` - TeaVM plugin registration
- `docs/FIELD_ACCESSOR_MECHANISM.md` - Complete documentation
