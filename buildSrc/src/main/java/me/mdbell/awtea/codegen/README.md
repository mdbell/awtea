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

**Usage as Gradle Plugin:**

The generator is available as a Gradle plugin that can be applied to any module:

```kotlin
plugins {
    id("java")
    id("deno-test-runner")
}

dependencies {
    testImplementation(project(":awtea-test-util"))
}
```

The plugin automatically:
- Creates a `generateDenoJUnitRunner` task
- Scans test sources for `@Test`, `@BeforeAll`, `@AfterAll`, `@BeforeEach`, `@AfterEach` annotations
- Generates `DenoJUnitRunner.java` in `build/generated/test/java`
- Detects the correct package name from existing test files
- Hooks into `compileTestJava` to run generation first

**Usage as Standalone Tool:**
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

## Gradle Plugins

### DenoTestRunnerPlugin

A reusable Gradle plugin that adds Deno test runner generation to any module.

**Location:** `buildSrc/src/main/kotlin/DenoTestRunnerPlugin.kt`

**Application:**
```kotlin
plugins {
    id("java")
    id("deno-test-runner")
}
```

**What it does:**
1. Registers `generateDenoJUnitRunner` task
2. Automatically detects package name from test files
3. Adds generated sources to test source set
4. Makes `compileTestJava` depend on generation

**Benefits:**
- No need to manually configure generation task in each module
- Consistent behavior across all modules
- Package name auto-detection reduces configuration
- Can be applied to any module that has Deno tests

## Integration with Gradle

### Using EnumGenerator

Invoked from Gradle build files using `JavaExec` tasks:

```kotlin
tasks.register<JavaExec>("generateEnums") {
    classpath = buildscript.configurations["classpath"]
    mainClass.set("me.mdbell.awtea.codegen.EnumGenerator")
    
    args(
        "--schemas", schemasDir.absolutePath,
        "--output-c", outputCDir.absolutePath,
        "--output-java", outputJavaDir.absolutePath,
        "--output-ts", outputTsDir.absolutePath,
        "--root-dir", rootDir.absolutePath
    )
}
```

### Using DenoTestRunnerPlugin

Simply apply the plugin to your module:

```kotlin
plugins {
    id("java")
    id("deno-test-runner")
}
```

No additional configuration needed! The plugin handles everything automatically.

## Adding New Generators

To add a new code generator:

1. Create a new Java class in this package
2. Implement a `main` method that accepts command-line arguments
3. Use consistent argument patterns (`--flag value`)
4. Print progress messages to stdout
5. Exit with non-zero status on errors
6. Consider creating a Gradle plugin wrapper for easier reuse
7. Document the generator in this README

## Adding New Plugins

To add a new Gradle plugin:

1. Create a Kotlin file in `buildSrc/src/main/kotlin/`
2. Implement `Plugin<Project>` interface
3. Create a properties file in `buildSrc/src/main/resources/META-INF/gradle-plugins/`
   - Filename: `your-plugin-id.properties`
   - Content: `implementation-class=YourPluginClassName`
4. Document the plugin in this README

## Dependencies

Generators can use:
- Java 11+ standard library
- Dependencies declared in `buildSrc/build.gradle.kts`
- Currently includes: SnakeYAML for YAML parsing
- Kotlin DSL for Gradle plugins

Avoid adding heavy dependencies to keep build times fast.
