import me.mdbell.awtea.codegen.DenoTestRunnerGenerator
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import java.io.File

/**
 * Gradle plugin that adds Deno test runner generation tasks to a project.
 * 
 * This plugin automatically:
 * - Creates a generateDenoJUnitRunner task
 * - Scans test sources for @Test annotations
 * - Generates DenoJUnitRunner.java in build/generated/test/java
 * - Hooks into compileTestJava to run generation first
 * 
 * Apply this plugin to any module that has Deno tests:
 * ```
 * plugins {
 *     id("deno-test-runner")
 * }
 * ```
 */
class DenoTestRunnerPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        
        // Register the test runner generation task
        project.tasks.register("generateDenoJUnitRunner") {
            description = "Generate DenoJUnitRunner.java from @Test annotations"
            group = "verification"
            
            val testSrcDir = project.file("src/test/java")
            val generatedSourceDir = project.layout.buildDirectory.dir("generated/test/java").get().asFile
            
            // Determine package name from project structure
            val packageName = determinePackageName(project, testSrcDir)
            val packagePath = packageName.replace('.', '/')
            val outputFile = File(generatedSourceDir, "$packagePath/DenoJUnitRunner.java")
            
            inputs.dir(testSrcDir)
            outputs.file(outputFile)
            
            doLast {
                val generator = DenoTestRunnerGenerator()
                generator.generate(
                    testSrcDir.toPath(),
                    outputFile.toPath(),
                    packageName
                )
            }
        }
        
        // Add generated sources to test source set
        sourceSets.named("test") {
            java.srcDir(project.layout.buildDirectory.dir("generated/test/java"))
        }
        
        // Make compileTestJava depend on generation
        project.tasks.named("compileTestJava") {
            dependsOn("generateDenoJUnitRunner")
        }
    }
    
    /**
     * Determine the package name for generated test runner based on project structure
     */
    private fun determinePackageName(project: Project, testSrcDir: File): String {
        // Try to find a test file to extract package name
        val testFiles = testSrcDir.walkTopDown()
            .filter { it.isFile && it.extension == "java" && it.name.contains("Test") }
            .take(1)
            .toList()
        
        if (testFiles.isNotEmpty()) {
            val content = testFiles[0].readText()
            val packageRegex = Regex("""package\s+([\w.]+);""")
            val match = packageRegex.find(content)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        
        // Fallback: use project name to construct package
        val projectName = project.name.removePrefix("awtea-")
        return "me.mdbell.awtea.$projectName.test"
    }
}
