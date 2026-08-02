plugins {
    id("java")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }

    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

group = "me.mdbell"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    implementation("org.teavm:teavm-core:0.15.0")
    implementation("org.teavm:teavm-classlib:0.15.0")
    implementation("org.teavm:teavm-jso-apis:0.15.0")
    
    // ClassGraph for class scanning
    implementation("io.github.classgraph:classgraph:4.8.177")
}
