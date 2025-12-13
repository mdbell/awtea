# Logging Migration Guide

## Quick Reference

This document provides guidelines for migrating remaining System.out/err usages to the unified logging framework.

## Migration Status

Total System.out/err occurrences: 159
- ✅ Migrated: 56 (35%)
- ⏳ Remaining: 103 (65%)

## Completed Modules

- ✅ awtea-instrument (6 occurrences)
- ✅ awtea-graphics/TtfDump (29 occurrences)
- ✅ awtea-util (16 occurrences, excluding ApiDiff.java)
- ✅ awtea-ui (5 occurrences)
- ✅ awtea-sound (2 occurrences)

## Remaining Work

### High Priority
1. **awtea-classlib** (~20 occurrences)
   - TEventManager, TComponent, TFont
   - Sound/MIDI classes: TAbstractSourceDataLine, TMidiJsSequencer

2. **awtea-core** (~10 occurrences)
   - IDBUtils, IndexedDBHelper, FileBenchmark

3. **awtea-graphics** (~15 occurrences)
   - FontLoader, TrueTypeFont
   - Rasterizers: WebGLRasterizer, WasmRasterizer, SurfaceLRUCache, DefaultSurfaceBackend

### Large File (Special Attention Needed)
4. **awtea-util/ApiDiff.java** (~53 occurrences)
   - This is a CLI tool with extensive console output
   - Requires careful manual migration to preserve formatting
   - Consider keeping as INFO level for normal output, DEBUG for verbose details

## Migration Steps

### 1. Import Logger
```java
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
```

### 2. Add Logger Field
```java
private static final Logger log = LoggerFactory.getLogger(YourClass.class);
```

### 3. Replace System.out/err Calls

#### Simple println
```java
// Before
System.out.println("Message");

// After
log.info("Message");
```

#### With string concatenation
```java
// Before
System.out.println("Count: " + count);

// After
log.info("Count: {}", count);
```

#### With printf formatting
```java
// Before
System.out.printf("Count: %d, Rate: %.2f%%\n", count, rate);

// After
log.info("Count: %d, Rate: %.2f%%", count, rate);
```

#### Error messages
```java
// Before
System.err.println("Error: " + message);

// After
log.error("Error: {}", message);
```

#### With exceptions
```java
// Before
try {
    // ...
} catch (Exception e) {
    System.err.println("Failed: " + e.getMessage());
    e.printStackTrace();
}

// After
try {
    // ...
} catch (Exception e) {
    log.error("Failed", e);  // Stack trace is automatically included
}
```

## Choosing Log Levels

| Level | Use Case | Examples |
|-------|----------|----------|
| ERROR | Critical errors, exceptions | Database connection failures, file I/O errors |
| WARN | Unexpected situations that don't prevent operation | Deprecated API usage, buffer overruns, fallback behaviors |
| INFO | Important business events | User actions, system state changes, configuration changes |
| DEBUG | Detailed diagnostic information | Event handling details, internal state changes, verbose tracing |

### Examples by Module

**awtea-classlib (UI components)**
- INFO: Component initialization, user actions
- DEBUG: Event handling, render cycles
- WARN: Invalid states, fallback behaviors
- ERROR: Critical failures

**awtea-core (Storage/DB)**
- INFO: Database operations, cache hits/misses
- DEBUG: Detailed operation traces
- WARN: Retry attempts, data inconsistencies
- ERROR: Connection failures, data corruption

**awtea-graphics (Rendering)**
- INFO: Backend initialization, resource loading
- DEBUG: Render operations, buffer management
- WARN: Performance issues, resource limits
- ERROR: Rendering failures, resource allocation errors

## Tips for ApiDiff.java Migration

ApiDiff.java is a CLI tool with ~53 System.out calls. Consider:

1. **Help/Usage Messages**: Keep as INFO
2. **Progress Messages**: Use INFO for user-facing progress
3. **Detailed Output**: Use DEBUG for verbose tracing
4. **Empty Lines**: Replace `System.out.println()` with `log.info("")` or consider if needed

Example approach:
```java
// Before
System.out.println("Usage: ApiDiff [options]");
System.out.println("Options:");
System.out.println("  --format <html|markdown>");

// After
log.info("Usage: ApiDiff [options]");
log.info("Options:");
log.info("  --format <html|markdown>");
```

## Testing After Migration

1. Compile the module: `./gradlew :module-name:compileJava`
2. Run full build: `./gradlew build -x test`
3. Verify log output appears correctly
4. Check that no System.out/err remain: `grep -rn "System\.\(out\|err\)\." src/main/java/`

## Pull Request Checklist

- [ ] Added Logger import and field
- [ ] Replaced all System.out/err in the file
- [ ] Used appropriate log levels
- [ ] Verified code compiles
- [ ] Tested logging output (if possible)
- [ ] Updated commit message with number of replacements

## Questions?

Refer to [LOGGING.md](LOGGING.md) for comprehensive documentation on the logging framework.
