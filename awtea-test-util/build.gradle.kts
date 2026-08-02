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
    implementation("org.teavm:teavm-jso-apis:0.15.0")
}
