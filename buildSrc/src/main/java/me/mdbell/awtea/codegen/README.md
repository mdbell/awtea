# Code Generation

This directory contains code generators used during the build process.

## Generators

### EnumGenerator

Generates enum definitions from YAML schema files for C, Java, and TypeScript.

**Usage:**
```bash
java me.mdbell.awtea.codegen.EnumGenerator \
  --schemas <schemas-dir> \
  --output-c <c-output-dir> \
  --output-java <java-output-dir> \
  --output-ts <ts-output-dir> \
  --root-dir <project-root>
```

**Schema Format:**
YAML files in the schemas directory define enums with:
- `name`: Enum name
- `description`: Documentation
- `values`: List of enum values with optional descriptions and explicit values
- `java_package`, `java_prefix`, `java_name`: Java-specific configuration
- `c_only`, `ts_only`, `java_skip`: Platform-specific value filtering

**Example:**
```yaml
name: LogLevel
description: Logging severity levels
java_package: me.mdbell.awtea.util
values:
  - name: DEBUG
    value: 0
    description: Debug messages
  - name: INFO
    value: 1
    description: Informational messages
```

### DenoTestRunnerGenerator

Generates `DenoJUnitRunner.java` from test classes with `@Test` annotations for Deno test execution.

**Usage:**
```bash
java me.mdbell.awtea.codegen.DenoTestRunnerGenerator \
  --test-src <test-source-dir> \
  --output <output-file> \
  --package <package-name>
```

**Features:**
- Scans test source files for `@Test`, `@BeforeAll`, `@AfterAll`, `@BeforeEach`, `@AfterEach` annotations
- Generates a runner that registers tests with Deno's test API via JSO
- Converts test method names from camelCase to readable names (e.g., `testPixelFormat` → "Pixel Format")
- Properly orders lifecycle methods (BeforeAll → BeforeEach → Test → AfterEach → AfterAll)

**Example Test Class:**
```java
import me.mdbell.awtea.test.*;

public class SurfaceTests {
    
    @BeforeAll
    public void setupAll() {
        // Run once before all tests
    }
    
    @BeforeEach
    public void setup() {
        // Run before each test
    }
    
    @Test
    public void testPixelFormat() {
        // Test code
    }
    
    @AfterEach
    public void teardown() {
        // Run after each test
    }
    
    @AfterAll
    public void teardownAll() {
        // Run once after all tests
    }
}
```

**Generated Output:**
```java
public class DenoJUnitRunner {
    public static void main(String[] args) {
        Deno.DenoAPI deno = Deno.getInstance();
        
        SurfaceTests surfaceTests = new SurfaceTests();
        surfaceTests.setupAll();
        
        deno.test("Java: Pixel Format", () -> { 
            surfaceTests.setup(); 
            surfaceTests.testPixelFormat(); 
            surfaceTests.teardown(); 
        });
        
        surfaceTests.teardownAll();
    }
}
```

## Integration with Gradle

Generators are invoked from Gradle build files using `JavaExec` tasks:

```kotlin
tasks.register<JavaExec>("generateDenoJUnitRunner") {
    classpath = buildscript.configurations["classpath"]
    mainClass.set("me.mdbell.awtea.codegen.DenoTestRunnerGenerator")
    
    args(
        "--test-src", testSrcDir.absolutePath,
        "--output", outputFile.absolutePath,
        "--package", "me.mdbell.awtea.gfx.test"
    )
}
```

## Adding New Generators

To add a new code generator:

1. Create a new Java class in this package
2. Implement a `main` method that accepts command-line arguments
3. Use consistent argument patterns (`--flag value`)
4. Print progress messages to stdout
5. Exit with non-zero status on errors
6. Document the generator in this README

## Dependencies

Generators can use:
- Java 11+ standard library
- Dependencies declared in `buildSrc/build.gradle.kts`
- Currently includes: SnakeYAML for YAML parsing

Avoid adding heavy dependencies to keep build times fast.
