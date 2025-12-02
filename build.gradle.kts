import org.teavm.gradle.api.JSModuleType

plugins {
    id ("java")
    id("org.teavm") version "0.11.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }

    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

teavm {
    all {
        debugInformation = true
    }
    js {
        moduleType = JSModuleType.ES2015
    }
}

repositories {
    mavenCentral()
}

dependencies {

    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    implementation("org.teavm:teavm-core:0.11.0")
    implementation("org.teavm:teavm-classlib:0.11.0")
    implementation("org.teavm:teavm-jso-apis:0.11.0")
}

tasks.test {
    useJUnitPlatform()
}