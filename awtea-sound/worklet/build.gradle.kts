import org.teavm.gradle.api.JSModuleType
import org.teavm.gradle.api.OptimizationLevel

plugins {
    id("java")
    id("org.teavm") version "0.13.1"
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
    implementation("org.teavm:teavm-core:0.13.1")
    implementation("org.teavm:teavm-classlib:0.13.1")
    implementation("org.teavm:teavm-jso-apis:0.13.1")

    implementation(project(":awtea-util"))
}

teavm {
    js {

        mainClass = "me.mdbell.awtea.sound.worklet.PcmProcessor"

        // Output directory for generated JavaScript
        outputDir = file("${project.parent?.projectDir}/build/resources/main")

        // we don't actually care about obfuscation here, this just
        // emits smaller code
        obfuscated = true
        optimization = OptimizationLevel.AGGRESSIVE;

        // we want a single JS file with no module system
        // so main() will be invoked when loaded in the worklet
        moduleType = JSModuleType.NONE

        // Entry point file name
        targetFileName = "pcm-processor.js"
    }
}

tasks.named("generateJavaScript") {
    doLast {
        val outputFile = file("${project.parent?.projectDir}/build/resources/main/js/pcm-processor.js")
        outputFile.appendText("\nmain();\n")
    }
}

// The worklet is JS-only by construction (it runs inside an AudioWorklet, which
// hosts a JS script). Disable the plugin's other targets so an aggregate
// `gradlew generateWasmGC` doesn't fail on this subproject's unset mainClass.
listOf("generateWasmGC", "generateWasm", "generateWasi", "generateC").forEach { name ->
    tasks.matching { it.name == name }.configureEach { enabled = false }
}