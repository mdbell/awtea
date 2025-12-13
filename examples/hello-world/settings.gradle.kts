rootProject.name = "hello-world"

// Include parent awtea project as a composite build
includeBuild("../..")

// Enable Gradle plugin portal for TeaVM plugin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
