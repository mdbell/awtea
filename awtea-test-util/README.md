# awtea-test-util

Shared test utilities for running Java tests in Deno via TeaVM compilation.

## Overview

This module provides a lightweight testing framework that replaces JUnit for TeaVM/Deno tests. It includes:

- **`@Test` annotation** - Marks test methods for auto-discovery
- **`Assert` class** - Assertion utilities that throw `AssertionError` on failure
- **`Deno` wrapper** - JSO integration with Deno's test API

## Usage

### 1. Add Dependency

Add to your module's `build.gradle.kts`:

```kotlin
dependencies {
    testImplementation(project(":awtea-test-util"))
}
```

### 2. Write Tests

```java
import me.mdbell.awtea.test.Test;
import static me.mdbell.awtea.test.Assert.*;

public class MyTests {
    
    @Test
    public void testSomething() {
        assertEquals(42, calculateAnswer(), "Answer should be 42");
        assertTrue(isValid(), "Should be valid");
    }
}
```

### 3. Generate Test Runner

The build system will automatically:
1. Scan for `@Test` methods
2. Generate a `DenoJUnitRunner` class
3. Register tests with Deno

## API Reference

### `@Test` Annotation

Marks a public void method as a test.

### `Assert` Class

Static assertion methods:

- `assertEquals(int actual, int expected, String msg)` - Assert equality
- `assertEquals(Object actual, Object expected, String msg)` - Assert object equality  
- `assertTrue(boolean expr, String msg)` - Assert true
- `assertFalse(boolean expr, String msg)` - Assert false
- `assertNotNull(Object obj, String msg)` - Assert not null
- `assertNull(Object obj, String msg)` - Assert null

All methods throw `AssertionError` on failure, which Deno's test framework catches and reports.

### `Deno` Class

JSO wrapper for Deno's test API:

```java
Deno.DenoAPI deno = Deno.getInstance();
deno.test("Test name", () -> {
    // test code
});
```

## Design

This module is designed to be:

- **Lightweight** - No heavy dependencies, just TeaVM JSO APIs
- **Reusable** - Can be used by any awtea module
- **Simple** - Plain Java assertions without complex frameworks
- **TeaVM-friendly** - No runtime reflection, works with TeaVM's limitations

The custom `@Test` annotation is discovered at build time via source file scanning, not runtime reflection.
