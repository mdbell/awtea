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

// Copy the HTML template to the dist directory
tasks.register<Copy>("copyWebapp") {
    from("src/main/webapp")
    into(layout.buildDirectory.dir("dist"))

    from("../../fonts")
    into(layout.buildDirectory.dir("dist/fonts"))
}

// Make sure HTML is copied before TeaVM runs
tasks.named("generateJavaScript") {
    dependsOn("copyWebapp")
}

tasks.named("build") {
    dependsOn("generateJavaScript")
}
