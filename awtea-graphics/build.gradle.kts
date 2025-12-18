plugins {
    id("java")
    id("deno-test-runner")
}

import java.net.URL
import java.net.URLClassLoader
import org.gradle.api.tasks.testing.Test

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
    
    testImplementation(project(":awtea-test-util"))
    testImplementation(project(":awtea-classlib"))  // For TeaVM to access AWT implementations
    testImplementation("org.teavm:teavm-tooling:0.13.0")
    testImplementation("org.teavm:teavm-platform:0.13.0")
    testImplementation("org.teavm:teavm-jso-impl:0.13.0")
    testImplementation("org.teavm:teavm-metaprogramming-impl:0.13.0")
}

var wasmOutputDir = file(layout.buildDirectory.dir("wasm"))

var nativeSrcDir = file("${projectDir}/src/main/native/")

// Generate enums before building
tasks.named("compileJava") {
    dependsOn(rootProject.tasks.named("generateEnums"))
}

tasks.register("buildAwtRasterWasm") {

    dependsOn(rootProject.tasks.named("generateEnums"))

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
                "emcc",
                "-Isrc/main/native",
                * sourceList.toTypedArray(),   // ← all .c sources here
                "-O2",
                "-s", "STANDALONE_WASM",
                "-s", "WASM_BIGINT=1",
                "-s", "ERROR_ON_UNDEFINED_SYMBOLS=0",
                "-s", "INITIAL_MEMORY=134217728", // 128MB
                "-DENABLE_WASM_STACK_TRACKING=1",  // Enable stack tracking by default
                "--no-entry",
                "-o", "build/wasm/awt_raster.wasm"
            )
        }
    }
}

// Configuration property to control WASM compilation (default: false)
// Enable with: ./gradlew build -PbuildWasm=true
val buildWasm = project.findProperty("buildWasm")?.toString()?.toBoolean() ?: false

tasks.named<ProcessResources>("processResources") {
    // Conditionally depend on WASM build if enabled
    if (buildWasm) {
        dependsOn("buildAwtRasterWasm")
        
        from(wasmOutputDir) {
            include("awt_raster.wasm")
            into("") // root of resources
        }
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

// Test runner generation is handled by the deno-test-runner plugin

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
        tool.setObfuscated(false)
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
        
        println("Generating JavaScript with TeaVM...")
        println("Main class: ${tool.mainClass}")
        println("Output: ${outputDir}/classes.js")
        
        tool.generate()
        
        val maxProblemsToShow = 20
        
        // Check for problems
        if (tool.problemProvider.severeProblems.isNotEmpty()) {
            println("ERROR: TeaVM encountered ${tool.problemProvider.severeProblems.size} severe problems:")
            tool.problemProvider.severeProblems.take(maxProblemsToShow).forEach {
                println("  Severity: ${it.severity}")
                println("  Location: ${it.location}")
                println("  Text: ${it.text}")
                if (it.params != null && it.params.isNotEmpty()) {
                    println("  Params: ${it.params.joinToString(", ")}")
                }
                println()
            }
            if (tool.problemProvider.severeProblems.size > maxProblemsToShow) {
                println("  ... and ${tool.problemProvider.severeProblems.size - maxProblemsToShow} more problems")
            }
            throw GradleException("TeaVM compilation failed with severe problems")
        }
        
        if (tool.problemProvider.problems.isNotEmpty()) {
            println("WARNING: TeaVM encountered problems:")
            tool.problemProvider.problems.forEach {
                println("  - ${it.location}: ${it.text}")
            }
        }
        
        val outputFile = File(outputDir, "classes.js")
        println("Generated JavaScript test bundle: ${outputDir}/classes.js (${outputFile.length()} bytes)")
    }
}

// Run Java tests compiled to JS with Deno
tasks.register<Exec>("denoTestJava") {
    description = "Run Java tests compiled to JS with Deno"
    group = "verification"
    
    dependsOn("buildDenoJavaTests")
    dependsOn("buildAwtRasterWasm")  // Ensure WASM file is built
    
    // Run from module root so Java code can find WASM at build/wasm/awt_raster.wasm
    workingDir = projectDir
    
    // Use absolute path to the test file and allow file reading
    val testFile = file("src/test/deno/java_tests.ts").absolutePath
    commandLine("deno", "test", "-A", testFile)
    
    inputs.files(sourceSets["test"].allSource)
    inputs.file("${layout.buildDirectory.get()}/deno-tests/classes.js")
    inputs.file("build/wasm/awt_raster.wasm")
    
    // Set environment variable to help with file URL resolution
    environment("DENO_DIR", layout.buildDirectory.dir(".deno").get().asFile.absolutePath)
    environment("DENO_JOBS", "1")
}

// Integrate Deno tests with the standard test task
tasks.named<Test>("test") {
    // Skip standard JUnit test execution since tests run via Deno
    enabled = false
    
    // Make test depend on denoTestJava to run Deno tests
    dependsOn("denoTestJava")
    dependsOn("denoTest")
}
