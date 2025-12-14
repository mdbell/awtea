# awtea-test-util

Shared test utilities for running Java tests in Deno via TeaVM compilation.

## Overview

This module provides a lightweight testing framework that replaces JUnit for TeaVM/Deno tests. It includes:

- **`@Test` annotation** - Marks test methods for auto-discovery
- **`@BeforeAll` annotation** - Marks methods to run once before all tests
- **`@AfterAll` annotation** - Marks methods to run once after all tests
- **`@BeforeEach` annotation** - Marks methods to run before each test
- **`@AfterEach` annotation** - Marks methods to run after each test
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
import me.mdbell.awtea.test.*;
import static me.mdbell.awtea.test.Assert.*;

public class MyTests {
    
    private int testCounter = 0;
    
    @BeforeAll
    public void setupAll() {
        System.out.println("Setting up test suite");
    }
    
    @AfterAll
    public void teardownAll() {
        System.out.println("Tearing down test suite");
    }
    
    @BeforeEach
    public void setup() {
        testCounter++;
        System.out.println("Starting test #" + testCounter);
    }
    
    @AfterEach
    public void teardown() {
        System.out.println("Completed test #" + testCounter);
    }
    
    @Test
    public void testSomething() {
        assertEquals(42, calculateAnswer(), "Answer should be 42");
        assertTrue(isValid(), "Should be valid");
    }
}
```

### 3. Generate Test Runner

The build system will automatically:
1. Scan for `@Test` methods and lifecycle annotations
2. Generate a `DenoJUnitRunner` class with proper lifecycle calls
3. Register tests with Deno

## API Reference

### Annotations

#### `@Test`
Marks a public void method as a test.

#### `@BeforeAll`
Marks a method to be run once before all tests in the class. The method must be public, void, and take no parameters.

#### `@AfterAll`
Marks a method to be run once after all tests in the class. The method must be public, void, and take no parameters.

#### `@BeforeEach`
Marks a method to be run before each test method. The method must be public, void, and take no parameters.

#### `@AfterEach`
Marks a method to be run after each test method. The method must be public, void, and take no parameters.

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

## Lifecycle Execution Order

For each test class:

1. **@BeforeAll** - Called once when the test class is initialized
2. For each test:
   - **@BeforeEach** - Called before the test
   - **@Test** - The test method executes
   - **@AfterEach** - Called after the test
3. **@AfterAll** - Called once after all tests complete

## Design

This module is designed to be:

- **Lightweight** - No heavy dependencies, just TeaVM JSO APIs
- **Reusable** - Can be used by any awtea module
- **Simple** - Plain Java assertions without complex frameworks
- **TeaVM-friendly** - No runtime reflection, works with TeaVM's limitations

The annotations are discovered at build time via source file scanning, not runtime reflection.
