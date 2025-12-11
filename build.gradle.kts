import org.teavm.gradle.api.JSModuleType
import org.teavm.gradle.api.OptimizationLevel

plugins {
    id("java")
    id("maven-publish")
    id("org.teavm") version "0.13.0"
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
}

teavm {
    all {
        debugInformation = true
    }
    js {
        moduleType = JSModuleType.ES2015
        mainClass = "me.mdbell.awtea.gfx.wasm.WasmTest"
        optimization = OptimizationLevel.NONE
        obfuscated = false
        debugInformation = true
        sourceMap = true
    }
}

var wasmOutputDir = file(layout.buildDirectory.dir("wasm"))

var nativeSrcDir = file("${projectDir}/src/main/native/c")

tasks.register("buildAwtRasterWasm", Exec::class.java) {
    group = "build"
    description = "Builds the AWT raster C code into WebAssembly using Emscripten in Docker."

    inputs.file("$nativeSrcDir/awt_raster.c")
    outputs.file("$wasmOutputDir/awt_raster.wasm")

    doFirst {
        wasmOutputDir.mkdirs()
    }

    commandLine(
        "docker", "run", "--rm",
        "-v", "${projectDir}:/src",
        "-w", "/src",
        "emscripten/emsdk",
        "emcc", "src/main/native/c/awt_raster.c", "-O2",
        "-s", "STANDALONE_WASM",
        "-s", "WASM_BIGINT=1",
        "--no-entry",
        "-o", "/src/build/wasm/awt_raster.wasm"
    )
}

tasks.named<ProcessResources>("processResources") {
    dependsOn("buildAwtRasterWasm")

    from(wasmOutputDir) {
        include("awt_raster.wasm")
        into("") // root of resources
    }
}
