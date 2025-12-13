plugins {
    id("java")
    id("org.teavm") version "0.13.0" apply false
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
    
    // Only apply maven-publish to non-example projects
    if (!project.path.startsWith(":examples")) {
        apply(plugin = "maven-publish")
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(11))
        }

        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Only configure publishing for non-example projects
    if (!project.path.startsWith(":examples")) {
        publishing {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                    artifactId = project.name
                }
            }
        }
    }
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
