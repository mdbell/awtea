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

        // Clean previous reports
        val reportDir = file("docs/coverage")
        if (reportDir.exists()) {
            reportDir.deleteRecursively()
        }
        reportDir.mkdirs()

        // Generate HTML report
        args("--format", "html")
        exec();
        args("--missing-classes", "--format", "html")
        exec();
    }

    doLast {
        // Generate Markdown report in a separate execution
        project.javaexec {
            classpath = project(":awtea-util").sourceSets["main"].runtimeClasspath
            mainClass.set("me.mdbell.awtea.util.ApiDiff")
            args("--format", "markdown")
            exec();
            args("--missing-classes", "--format", "markdown")
            exec();
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
