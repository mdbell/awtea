import org.teavm.gradle.api.JSModuleType
import org.teavm.gradle.api.SourceFilePolicy

plugins {
    id("java")
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

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.teavm:teavm-core:0.13.0")
    implementation("org.teavm:teavm-classlib:0.13.0")
    implementation("org.teavm:teavm-jso-apis:0.13.0")
}

teavm {
    // Main compilation target - JavaScript for browser
    js {

        // Your main class entry point
        mainClass = "me.mdbell.awtea.sound.worklet.PcmProcessor"

        // Output directory for generated JavaScript
        outputDir = file("${project.parent?.projectDir}/build/resources/main")

        // Optimization level:  SIMPLE, ADVANCED, or FULL
        // SIMPLE = faster builds, larger output
        // FULL = slower builds, smaller output
        obfuscated = false  // Set to true for production
//        optimization = OptimizationLevel,

        // Source maps for debugging
        sourceMap = true
        debugInformation = true

        sourceFilePolicy = SourceFilePolicy.COPY

        // we want a single JS file with no module system
        // so main() will be invoked when loaded in the worklet
        moduleType = JSModuleType.NONE

        // Entry point file name
        targetFileName = "pcm-processor.js"
    }
}