plugins {
    id("java")
}

import java.net.URL
import java.net.URLClassLoader

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }

    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

group = "me.mdbell"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    implementation("org.teavm:teavm-core:0.13.0")
    implementation("org.teavm:teavm-classlib:0.13.0")
    implementation("org.teavm:teavm-jso-apis:0.13.0")
    
    implementation(project(":awtea-instrument"))
    implementation(project(":awtea-util"))
    
    testImplementation("org.teavm:teavm-junit:0.13.0")
    testImplementation("org.teavm:teavm-tooling:0.13.0")
    testImplementation("org.teavm:teavm-platform:0.13.0")
    testImplementation("org.teavm:teavm-jso-impl:0.13.0")
    testImplementation("org.teavm:teavm-metaprogramming-impl:0.13.0")
    testImplementation("junit:junit:4.13.2")
}

var wasmOutputDir = file(layout.buildDirectory.dir("wasm"))

var nativeSrcDir = file("${projectDir}/src/main/native/")

tasks.register("buildAwtRasterWasm") {

    // 1. Collect all .c files
    val cSources = fileTree(nativeSrcDir) {
        include("**/*.c")
    }.files

    val headerFiles = fileTree(nativeSrcDir) {
        include("**/*.h")
    }.files

    // 2. Declare inputs
    inputs.files(cSources)
    inputs.files(headerFiles)

    // 3. Declare output
    outputs.file("$projectDir/build/wasm/awt_raster.wasm")

    doLast {
        val sourceList = cSources.map { "src/main/native/${it.name}" }

        exec {
            commandLine(
                "docker", "run", "--rm",
                "-v", "${projectDir}:/src",
                "-w", "/src",
                "emscripten/emsdk",
                "emcc",
                "-Isrc/main/native",
                * sourceList.toTypedArray(),   // ← all .c sources here
                "-O2",
                "-s", "STANDALONE_WASM",
                "-s", "WASM_BIGINT=1",
                "-s", "ERROR_ON_UNDEFINED_SYMBOLS=0",
                "--no-entry",
                "-o", "build/wasm/awt_raster.wasm"
            )
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn("buildAwtRasterWasm")

    from(wasmOutputDir) {
        include("awt_raster.wasm")
        into("") // root of resources
    }
}

// Deno test task for WASM rasterizer isolated testing
tasks.register<Exec>("denoTest") {
    description = "Run Deno tests for WASM rasterizer in isolation"
    group = "verification"
    
    dependsOn("buildAwtRasterWasm")
    
    workingDir = file("src/test/deno")
    
    commandLine("deno", "test", "--allow-read")
    
    // Make the task cacheable
    inputs.files(fileTree("src/test/deno") {
        include("*.ts")
    })
    inputs.file("build/wasm/awt_raster.wasm")
}

// Optional: Hook into the check task to run Deno tests
// Uncomment the following line to run Deno tests as part of standard Gradle checks
// tasks.named("check") {
//     dependsOn("denoTest")
// }

// Generate DenoJUnitRunner.java based on @Test annotations
tasks.register("generateDenoJUnitRunner") {
    description = "Generate DenoJUnitRunner.java from @Test annotations"
    group = "verification"
    
    val generatedSourceDir = file("${layout.buildDirectory.get()}/generated/test/java")
    val outputFile = file("${generatedSourceDir}/me/mdbell/awtea/gfx/test/DenoJUnitRunner.java")
    
    inputs.files(sourceSets["test"].allJava)
    outputs.file(outputFile)
    
    doLast {
        // Find all test classes with @Test methods
        val testMethods = mutableListOf<Pair<String, String>>()
        
        sourceSets["test"].allJava.forEach { srcFile ->
            if (srcFile.name.endsWith(".java") && srcFile.name.contains("Test")) {
                val content = srcFile.readText()
                
                // Skip the generated runner itself
                if (content.contains("class DenoJUnitRunner")) {
                    return@forEach
                }
                
                // Simple parsing to find class name and @Test methods
                val classNameMatch = Regex("""public\s+class\s+(\w+)""").find(content)
                if (classNameMatch != null) {
                    val className = classNameMatch.groupValues[1]
                    val packageMatch = Regex("""package\s+([\w.]+);""").find(content)
                    val fullClassName = if (packageMatch != null) {
                        "${packageMatch.groupValues[1]}.${className}"
                    } else {
                        className
                    }
                    
                    // Find all @Test annotated methods
                    val methodPattern = Regex("""@Test\s+public\s+void\s+(\w+)\s*\(\s*\)""")
                    methodPattern.findAll(content).forEach { match ->
                        val methodName = match.groupValues[1]
                        testMethods.add(Pair(fullClassName, methodName))
                    }
                }
            }
        }
        
        // Generate the DenoJUnitRunner.java file
        outputFile.parentFile.mkdirs()
        outputFile.writeText("""
package me.mdbell.awtea.gfx.test;

/**
 * Auto-generated class that registers Java tests with Deno's test framework.
 * This file is generated by the generateDenoJUnitRunner Gradle task.
 * 
 * DO NOT EDIT MANUALLY - Changes will be overwritten.
 */
public class DenoJUnitRunner {
    
    public static void main(String[] args) {
        Deno.DenoAPI deno = Deno.getInstance();
        
${testMethods.groupBy { it.first }.map { (className, methods) ->
            val simpleClassName = className.substringAfterLast(".")
            val varName = simpleClassName.replaceFirstChar { it.lowercase() }
            """        // Register tests from ${className}
        ${className} ${varName} = new ${className}();
${methods.joinToString("\n") { (_, methodName) ->
                val testName = camelCaseToWords(methodName)
                """        deno.test("Java: ${testName}", () -> ${varName}.${methodName}());"""
            }}"""
        }.joinToString("\n        \n")}
    }
}
""".trimIndent())
        
        println("Generated DenoJUnitRunner with ${testMethods.size} tests")
    }
}

// Helper function to convert camelCase to human-readable words
fun camelCaseToWords(methodName: String): String {
    var name = methodName
    // Remove "test" prefix if present
    if (name.startsWith("test") && name.length > 4) {
        name = name.substring(4)
    }
    
    // Convert camelCase to space-separated words
    return name.replace(Regex("([A-Z])"), " $1")
        .trim()
        .replaceFirstChar { it.uppercase() }
}

// Add generated sources to test source set
sourceSets {
    test {
        java {
            srcDir("${layout.buildDirectory.get()}/generated/test/java")
        }
    }
}

// Ensure generation happens before compilation
tasks.named("compileTestJava") {
    dependsOn("generateDenoJUnitRunner")
}

// Compile Java tests to JavaScript using TeaVM for Deno execution
tasks.register("buildDenoJavaTests") {
    description = "Compile Java tests to JavaScript for Deno execution"
    group = "verification"
    
    dependsOn("testClasses")
    
    val outputDir = file("${layout.buildDirectory.get()}/deno-tests")
    
    inputs.files(sourceSets["test"].output, sourceSets["test"].runtimeClasspath)
    outputs.dir(outputDir)
    
    doLast {
        val tool = org.teavm.tooling.TeaVMTool()
        tool.targetDirectory = outputDir
        tool.setTargetFileName("classes.js")
        tool.mainClass = "me.mdbell.awtea.gfx.test.DenoJUnitRunner"
        tool.optimizationLevel = org.teavm.vm.TeaVMOptimizationLevel.SIMPLE
        tool.isSourceMapsFileGenerated = true
        tool.isDebugInformationGenerated = true
        tool.targetType = org.teavm.tooling.TeaVMTargetType.JAVASCRIPT
        tool.setJsModuleType(org.teavm.backend.javascript.JSModuleType.ES2015)
        
        // Create a classloader that includes both test runtime and TeaVM runtime
        val urls = mutableListOf<URL>()
        sourceSets["test"].runtimeClasspath.forEach { urls.add(it.toURI().toURL()) }
        sourceSets["test"].output.forEach { urls.add(it.toURI().toURL()) }
        
        val classLoader = URLClassLoader(urls.toTypedArray(), Thread.currentThread().contextClassLoader)
        tool.classLoader = classLoader
        
        tool.generate()
        println("Generated JavaScript test bundle: ${outputDir}/classes.js")
    }
}

// Run Java tests compiled to JS with Deno
tasks.register<Exec>("denoTestJava") {
    description = "Run Java tests compiled to JS with Deno"
    group = "verification"
    
    dependsOn("buildDenoJavaTests")
    
    workingDir = file("src/test/deno")
    commandLine("deno", "test", "--allow-read", "java_tests.ts")
    
    inputs.files(sourceSets["test"].allSource)
    inputs.file("${layout.buildDirectory.get()}/deno-tests/classes.js")
}
