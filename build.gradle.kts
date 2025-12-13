plugins {
    id("java")
    id("maven-publish")
}

group = "me.mdbell"
version = "0.1.0"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(11))
        }

        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                artifactId = project.name
            }
        }
    }
}

// Aggregate all submodules into a single JAR
dependencies {
    implementation(project(":awtea-core"))
    implementation(project(":awtea-classlib"))
    implementation(project(":awtea-instrument"))
    implementation(project(":awtea-graphics"))
    implementation(project(":awtea-sound"))
    implementation(project(":awtea-input"))
    implementation(project(":awtea-net"))
    implementation(project(":awtea-ui"))
    implementation(project(":awtea-util"))
}

tasks.register<JavaExec>("generateDocs") {
    group = "documentation"
    description = "Generate API coverage reports in HTML and Markdown formats"
    
    dependsOn(":awtea-util:classes")
    
    classpath = project(":awtea-util").sourceSets["main"].runtimeClasspath
    mainClass.set("me.mdbell.awtea.util.ApiDiff")
    
    doFirst {
        // Generate HTML report
        args("--format", "html")
    }
    
    doLast {
        // Generate Markdown report in a separate execution
        project.javaexec {
            classpath = project(":awtea-util").sourceSets["main"].runtimeClasspath
            mainClass.set("me.mdbell.awtea.util.ApiDiff")
            args("--format", "markdown")
        }
        
        println("✓ Generated HTML report: docs/coverage/report.html")
        println("✓ Generated Markdown report: docs/coverage/report.md")
    }
}

tasks.register<JavaExec>("findMissingClasses") {
    group = "documentation"
    description = "Find missing public classes in java.awt.* packages"
    
    dependsOn(":awtea-util:classes")
    
    classpath = project(":awtea-util").sourceSets["main"].runtimeClasspath
    mainClass.set("me.mdbell.awtea.util.ApiDiff")
    args("--missing-classes")
}
