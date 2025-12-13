plugins {
    id("java")
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
