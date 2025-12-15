# Field Accessor Mechanism for Detours

## Overview

The field accessor mechanism enables detour classes to access private fields of target classes without using reflection. This is essential for performance-critical code paths, especially in WASM rendering where zero-copy optimizations require direct field access.

## Motivation

When creating detours for performance-sensitive code, you often need to:
1. Read private fields to preserve existing behavior
2. Write private fields to optimize data storage (e.g., WASM-native arrays)
3. Avoid reflection overhead in hot code paths

The field accessor annotations (`@FieldAccessor` and `@FieldSetter`) solve this by generating compile-time JavaScript accessors that directly access object fields.

## Key Benefits

- **Zero-copy optimizations**: Access WASM-native data structures directly
- **No reflection overhead**: Generated code uses direct field access
- **Type-safe**: Compile-time validation of field types
- **Clean separation**: Detour code stays isolated from original classes

## Usage

### Basic Example

```java
@DetourReceiver(target = SomeClass.class)
public class SomeClassDetours {
    
    // Generate getter for private field
    @FieldAccessor("privateField")
    @GeneratedBy(FieldAccessorGenerator.class)
    private static native int getPrivateField(SomeClass instance);
    
    // Generate setter for private field
    @FieldSetter("privateField")
    @GeneratedBy(FieldAccessorGenerator.class)
    private static native void setPrivateField(SomeClass instance, int value);
    
    // Use field accessors in detour method
    @DetourMethod("someMethod")
    public static void someMethod(SomeClass self, int arg) {
        int current = getPrivateField(self);
        setPrivateField(self, current + arg);
    }
}
```

### Requirements for Field Accessor Methods

Field accessor methods must:
- Be `private static native`
- Be annotated with `@FieldAccessor` or `@FieldSetter`
- Be annotated with `@GeneratedBy(FieldAccessorGenerator.class)`
- Be declared in a class annotated with `@DetourReceiver`

#### Field Getter Requirements

```java
@FieldAccessor("fieldName")
@GeneratedBy(FieldAccessorGenerator.class)
private static native FieldType getFieldName(TargetType instance);
```

- Must have exactly one parameter (the target instance)
- Return type must match the field type
- Cannot return `void`

#### Field Setter Requirements

```java
@FieldSetter("fieldName")
@GeneratedBy(FieldAccessorGenerator.class)
private static native void setFieldName(TargetType instance, FieldType value);
```

- Must have exactly two parameters (target instance and new value)
- Second parameter type must match the field type
- Must return `void`

## Real-World Example: Zero-Copy BufferedImage

This example shows how to optimize pixel buffer allocation for WASM rendering:

```java
// Original applet code (requires data copy on every render)
public class BufferedPixMap extends PixMap {
    public void create(int width, int height, Component component) {
        this.width = width;
        this.height = height;
        // Standard Java array - NOT in WASM memory
        this.data = new int[width * height + 1];
        
        DataBufferInt buffer = new DataBufferInt(this.data, this.data.length);
        DirectColorModel model = new DirectColorModel(32, 0xff0000, 0xff00, 0xff);
        WritableRaster raster = Raster.createWritableRaster(
            model.createCompatibleSampleModel(this.width, this.height), 
            buffer, 
            null
        );
        this.image = new BufferedImage(model, raster, false, new Hashtable());
    }
}

// Optimized detour (zero-copy WASM rendering)
@DetourReceiver(target = BufferedPixMap.class)
public class BufferedPixMapDetour {
    
    @FieldAccessor("width")
    @GeneratedBy(FieldAccessorGenerator.class)
    private static native int getWidth(BufferedPixMap instance);
    
    @FieldSetter("width")
    @GeneratedBy(FieldAccessorGenerator.class)
    private static native void setWidth(BufferedPixMap instance, int width);
    
    @FieldAccessor("data")
    @GeneratedBy(FieldAccessorGenerator.class)
    private static native int[] getData(BufferedPixMap instance);
    
    @FieldSetter("data")
    @GeneratedBy(FieldAccessorGenerator.class)
    private static native void setData(BufferedPixMap instance, int[] data);
    
    @DetourMethod("create")
    public static void create(BufferedPixMap self, int width, int height, Component component) {
        setWidth(self, width);
        // Similar for height...
        
        // Create BufferedImage with WASM-native pixel buffer
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        // Get the data buffer - this returns an int[] aliased to Int32Array in WASM memory
        // Zero copies needed for rendering!
        int[] wasmNativeData = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();
        setData(self, wasmNativeData);
        
        // Continue with original logic...
    }
}
```

### Performance Impact

- **Before**: Every frame requires copying pixel data from Java array to WASM memory
- **After**: Pixel data lives directly in WASM memory, no copies needed
- **Result**: Significant speedup for real-time rendering (60+ FPS for large buffers)

## Generated JavaScript Code

The `FieldAccessorGenerator` produces compact JavaScript:

### Field Getter
```javascript
// Java: getPrivateField(instance)
// Generates:
return instance.privateField;
```

### Field Setter
```javascript
// Java: setPrivateField(instance, value)
// Generates:
instance.privateField = value;
```

## Integration with Detour System

Field accessors work seamlessly with the existing detour infrastructure:

1. **Detour Registration**: Detour classes are registered via `META-INF/awtea.detours`
2. **Method Detours**: Use `@DetourMethod` to replace original methods
3. **Field Access**: Use `@FieldAccessor`/`@FieldSetter` to access private fields
4. **Constructor Detours**: Use `@DetourMethod(constructor = true)` for factories

All annotations follow the same pattern and work together:

```java
@DetourReceiver(target = TargetClass.class)
public class TargetClassDetours {
    // Field accessors
    @FieldAccessor("field") @GeneratedBy(FieldAccessorGenerator.class)
    private static native Type getField(TargetClass instance);
    
    // Method detours
    @DetourMethod("method")
    public static ReturnType method(TargetClass self, ArgType arg) {
        Type field = getField(self);
        // Use field...
    }
}
```

## Limitations and Caveats

1. **JavaScript Field Names**: Generated code assumes field names in JavaScript match Java field names (TeaVM convention)
2. **No Validation**: Field existence is not validated at compile-time (only at runtime)
3. **Type Safety**: Return/parameter types must match field types (enforced by JavaScript runtime)
4. **Private Fields Only**: Designed for private fields; public fields should be accessed directly
5. **Single Instance Parameter**: Getters take one param, setters take two params (no varargs)

## Troubleshooting

### Compilation Errors

**Error**: `@FieldAccessor method must have exactly one parameter`
- **Fix**: Getter methods must have exactly one parameter (the target instance)

**Error**: `@FieldSetter method must return void`
- **Fix**: Setter methods must return `void`, not the field type

**Error**: `@FieldAccessor method's declaring class must be annotated with @DetourReceiver`
- **Fix**: Add `@DetourReceiver(target = TargetClass.class)` to the detour class

### Runtime Errors

**Error**: `instance.fieldName is undefined`
- **Fix**: Field name may be incorrect or field may not exist in JavaScript
- Check TeaVM field name mappings in generated JavaScript

**Error**: Type mismatch when accessing field
- **Fix**: Ensure accessor return type matches field type exactly

## See Also

- [DetourHacks.java](../awtea-instrument/src/main/java/me/mdbell/awtea/instrument/DetourHacks.java) - Method detour implementation
- [FieldAccessorExample.java](../awtea-instrument/src/main/java/me/mdbell/awtea/instrument/detour/FieldAccessorExample.java) - Example usage
- [COMPONENT_MAPPING.md](COMPONENT_MAPPING.md) - Component architecture and detour strategies
- [RENDERING_BACKENDS.md](RENDERING_BACKENDS.md) - WASM rendering backend details
