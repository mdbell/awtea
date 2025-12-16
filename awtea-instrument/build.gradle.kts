plugins {
    id("java")
    id("deno-test-runner")
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
    
    implementation(project(":awtea-core"))
    implementation(project(":awtea-util"))
    
    testImplementation(project(":awtea-test-util"))
    testImplementation("org.teavm:teavm-tooling:0.13.0")
    testImplementation("org.teavm:teavm-platform:0.13.0")
    testImplementation("org.teavm:teavm-jso-impl:0.13.0")
    testImplementation("org.teavm:teavm-metaprogramming-impl:0.13.0")
}

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
        tool.mainClass = "me.mdbell.awtea.instrument.test.DenoJUnitRunner"
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
    }
}

tasks.register<Exec>("denoTestJava") {
    description = "Run Java tests compiled to JS with Deno"
    group = "verification"
    
    dependsOn("buildDenoJavaTests")
    
    workingDir = file("src/test/deno")
    commandLine("deno", "test", "--allow-read", "java_tests.ts")
    
    inputs.files(sourceSets["test"].allSource)
    inputs.file("${layout.buildDirectory.get()}/deno-tests/classes.js")
}
