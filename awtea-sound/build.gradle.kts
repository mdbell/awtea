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

    implementation("org.teavm:teavm-core:0.13.1")
    implementation("org.teavm:teavm-jso-apis:0.13.1")

    implementation(project(":awtea-util"))
    implementation(project(":awtea-sound:worklet"))


}

tasks.compileJava {
    dependsOn("${project.path}:worklet:generateJavaScript")
}
