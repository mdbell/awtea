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
    implementation("org.teavm:teavm-jso-apis:0.13.0")
}
