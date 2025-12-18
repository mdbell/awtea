import org.teavm.gradle.api.JSModuleType
import org.teavm.gradle.api.OptimizationLevel

plugins {
    id("java")
    id("org.teavm")
}

group = "me.mdbell.awtea.examples"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    // awtea dependencies - now as project dependencies
    implementation(project(":awtea-classlib"))
    // needed for logging
    implementation(project(":awtea-util"))

    // TeaVM dependencies
    implementation("org.teavm:teavm-classlib:0.13.0")
    implementation("org.teavm:teavm-jso-apis:0.13.0")
}

teavm {
    js {
        // Configure JavaScript generation
        mainClass = "me.mdbell.awtea.examples.helloworld.HelloWorld"
        outputDir = layout.buildDirectory.dir("dist").get().asFile
        moduleType = JSModuleType.ES2015

        // Optimization settings
        optimization = OptimizationLevel.NONE
        obfuscated = false

        // Source maps for debugging
        sourceMap = true
    }
}

// Copy WASM rasterizer to dist directory
tasks.register<Copy>("copyWasmToWebapp") {
    description = "Copy WASM rasterizer to dist directory"
    group = "build"
    
    val wasmFile = rootProject.project(":awtea-graphics")
        .layout.buildDirectory.file("wasm/awt_raster.wasm")
    
    from(wasmFile)
    into(layout.buildDirectory.dir("dist"))
    
    dependsOn(":awtea-graphics:buildAwtRasterWasm")
    
    // Only run if WASM file exists or will be built
    onlyIf {
        wasmFile.get().asFile.exists() || gradle.taskGraph.hasTask(":awtea-graphics:buildAwtRasterWasm")
    }
}

// Copy the HTML template to the dist directory
tasks.register<Copy>("copyWebapp") {
    from("src/main/webapp")
    into(layout.buildDirectory.dir("dist"))

    from("../../webapp-common")
    into(layout.buildDirectory.dir("dist"))
    
    dependsOn("copyWasmToWebapp")
}

// Make sure HTML is copied before TeaVM runs
tasks.named("generateJavaScript") {
    dependsOn("copyWebapp")
    
    // Fix implicit dependency: ensure generateJavaScript runs after
    // generateDenoJUnitRunner in dependency projects (awtea-graphics)
    // This prevents Gradle task validation errors when running full builds
    project(":awtea-graphics").tasks.findByName("generateDenoJUnitRunner")?.let {
        mustRunAfter(it)
    }
}

tasks.named("build") {
    dependsOn("generateJavaScript")
}
