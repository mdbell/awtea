plugins {
    id("java")
    id("maven-publish")
//    id("org.teavm") version "0.13.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }

    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

group = "me.mdbell"
version = "0.1.0"

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "awtea"
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {

    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    implementation("org.teavm:teavm-core:0.13.0")
    implementation("org.teavm:teavm-classlib:0.13.0")
    implementation("org.teavm:teavm-jso-apis:0.13.0")

    // ClassGraph for class scanning
    implementation("io.github.classgraph:classgraph:4.8.177")
}

//teavm {
//    all {
//        debugInformation = true
//    }
//    js {
//        moduleType = JSModuleType.ES2015
//        mainClass = "me.mdbell.awtea.gfx.wasm.WasmTest"
//        optimization = OptimizationLevel.NONE
//        obfuscated = false
//        debugInformation = true
//        sourceMap = true
//    }
//}

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

tasks.register<JavaExec>("generateDocs") {
    group = "documentation"
    description = "Generate API coverage reports in HTML and Markdown formats"

    dependsOn("classes")

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("me.mdbell.awtea.util.ApiDiff")

    doFirst {

        // Clean previous reports
        val reportDir = file("docs/coverage")
        if (reportDir.exists()) {
            reportDir.deleteRecursively()
        }
        reportDir.mkdirs()

        // Generate HTML report
        args("--format", "html")
        exec();
        args("--missing-classes", "--format", "html")
        exec();
    }

    doLast {
        // Generate Markdown report in a separate execution
        project.javaexec {
            classpath = sourceSets["main"].runtimeClasspath
            mainClass.set("me.mdbell.awtea.util.ApiDiff")
            args("--format", "markdown")
            exec();
            args("--missing-classes", "--format", "markdown")
            exec();
        }

        println("✓ Generated HTML report: docs/coverage/report.html")
        println("✓ Generated Markdown report: docs/coverage/report.md")
    }
}

tasks.register<JavaExec>("findMissingClasses") {
    group = "documentation"
    description = "Find missing public classes in java.awt.* packages"

    dependsOn("classes")

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("me.mdbell.awtea.util.ApiDiff")
    args("--missing-classes")
}
