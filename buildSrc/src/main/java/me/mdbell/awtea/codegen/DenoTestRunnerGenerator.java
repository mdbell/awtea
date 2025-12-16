package me.mdbell.awtea.codegen;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Generates DenoJUnitRunner.java from test classes with @Test annotations
 */
public class DenoTestRunnerGenerator {

    /**
     * Main entry point for test runner generation
     */
    public static void main(String[] args) {
        if (args.length < 6 || args.length % 2 != 0) {
            System.err.println("Usage: DenoTestRunnerGenerator --test-src <dir> --output <file> --package <package>");
            System.exit(1);
        }

        Path testSrcDir = null;
        Path outputFile = null;
        String packageName = null;

        for (int i = 0; i < args.length; i += 2) {
            if (i + 1 >= args.length)
                break;
            String flag = args[i];
            String value = args[i + 1];

            switch (flag) {
                case "--test-src":
                    testSrcDir = Path.of(value);
                    break;
                case "--output":
                    outputFile = Path.of(value);
                    break;
                case "--package":
                    packageName = value;
                    break;
                default:
                    System.err.println("Unknown flag: " + flag);
                    System.exit(1);
            }
        }

        if (testSrcDir == null || outputFile == null || packageName == null) {
            System.err.println("Missing required arguments");
            System.exit(1);
        }

        try {
            new DenoTestRunnerGenerator().generate(testSrcDir, outputFile, packageName);
        } catch (Exception e) {
            System.err.println("Error generating test runner: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Generate the DenoJUnitRunner.java file
     */
    public void generate(Path testSrcDir, Path outputFile, String packageName) throws IOException {
        System.out.println("Scanning for test classes in: " + testSrcDir);

        Map<String, TestClassInfo> testClasses = scanTestClasses(testSrcDir);

        if (testClasses.isEmpty()) {
            System.out.println("No test classes found");
            return;
        }

        int totalTests = testClasses.values().stream()
                .mapToInt(info -> info.testMethods.size())
                .sum();

        System.out.println("Found " + totalTests + " tests in " + testClasses.size() + " test classes");

        Files.createDirectories(outputFile.getParent());

        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            writeTestRunner(writer, packageName, testClasses);
        }

        System.out.println("Generated test runner: " + outputFile);
    }

    /**
     * Scan test source directory for test classes
     */
    private Map<String, TestClassInfo> scanTestClasses(Path testSrcDir) throws IOException {
        Map<String, TestClassInfo> testClasses = new HashMap<>();

        if (!Files.exists(testSrcDir) || !Files.isDirectory(testSrcDir)) {
            return testClasses;
        }

        try (Stream<Path> paths = Files.walk(testSrcDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> p.getFileName().toString().contains("Test"))
                    .forEach(path -> {
                        try {
                            processTestFile(path, testClasses);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to process test file: " + path, e);
                        }
                    });
        }

        return testClasses;
    }

    /**
     * Process a single test file
     */
    private void processTestFile(Path file, Map<String, TestClassInfo> testClasses) throws IOException {
        String content = Files.readString(file);

        // Skip generated runner
        if (content.contains("class DenoJUnitRunner")) {
            return;
        }

        // Extract package
        Pattern packagePattern = Pattern.compile("package\\s+([\\w.]+);");
        Matcher packageMatcher = packagePattern.matcher(content);
        String packageName = packageMatcher.find() ? packageMatcher.group(1) : "";

        // Extract class name
        Pattern classPattern = Pattern.compile("public\\s+class\\s+(\\w+)");
        Matcher classMatcher = classPattern.matcher(content);
        if (!classMatcher.find()) {
            return;
        }

        String className = classMatcher.group(1);
        String fullClassName = packageName.isEmpty() ? className : packageName + "." + className;

        TestClassInfo classInfo = new TestClassInfo(fullClassName);

        // Find annotated methods
        findMethods(content, "@Test", classInfo.testMethods);

        if (!classInfo.testMethods.isEmpty()) {
            testClasses.put(fullClassName, classInfo);
        }
    }

    /**
     * Find methods with a specific annotation
     */
    private void findMethods(String content, String annotation, List<String> methodList) {
        Pattern pattern = Pattern.compile(annotation + "\\s+public\\s+void\\s+(\\w+)\\s*\\(\\s*\\)");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            methodList.add(matcher.group(1));
        }
    }

    /**
     * Write the test runner file
     */
    private void writeTestRunner(Writer writer, String packageName, Map<String, TestClassInfo> testClasses)
            throws IOException {
        writer.write("package " + packageName + ";\n\n");
        writer.write("import me.mdbell.awtea.test.Deno;\n\n");
        writer.write("/**\n");
        writer.write(" * Auto-generated class that registers Java tests with Deno's test framework.\n");
        writer.write(" * This file is generated by the DenoTestRunnerGenerator.\n");
        writer.write(" * \n");
        writer.write(" * DO NOT EDIT MANUALLY - Changes will be overwritten.\n");
        writer.write(" */\n");
        writer.write("public class DenoJUnitRunner {\n");
        writer.write("    \n");
        writer.write("    public static void main(String[] args) {\n");
        writer.write("        Deno.DenoAPI deno = Deno.getInstance();\n");
        writer.write("        Object sync = new Object();");
        writer.write("        \n");

        boolean first = true;
        for (TestClassInfo classInfo : testClasses.values()) {
            if (!first) {
                writer.write("        \n");
            }
            first = false;

            writeTestClass(writer, classInfo);
        }

        writer.write("    }\n");
        writer.write("}\n");
    }

    /**
     * Write test registration for a single test class
     */
    private void writeTestClass(Writer writer, TestClassInfo classInfo) throws IOException {
        String simpleClassName = classInfo.fullClassName.substring(classInfo.fullClassName.lastIndexOf('.') + 1);

        writer.write("        // Register tests from " + classInfo.fullClassName + "\n");
        int index = 0;

        // Test methods with BeforeEach/AfterEach
        for (String testMethod : classInfo.testMethods) {
            String varName = simpleClassName.substring(0, 1).toLowerCase() + simpleClassName.substring(1) + index;
            writer.write(
                    "        " + classInfo.fullClassName + " " + varName + " = new " + classInfo.fullClassName
                            + "();\n");
            String testName = camelCaseToWords(testMethod);
            writer.write("        deno.test(\"Java: " + testName + "\", () -> { ");
            writer.write(varName + "." + testMethod + "(); });\n");
            index++;
        }
    }

    /**
     * Convert camelCase to readable words
     */
    private String camelCaseToWords(String methodName) {
        String name = methodName;

        // Remove "test" prefix if present
        if (name.startsWith("test") && name.length() > 4) {
            name = name.substring(4);
        }

        // Convert camelCase to space-separated words
        String result = name.replaceAll("([A-Z])", " $1").trim();

        // Capitalize first letter
        if (!result.isEmpty()) {
            result = result.substring(0, 1).toUpperCase() + result.substring(1);
        }

        return result;
    }

    /**
     * Holds information about a test class
     */
    private static class TestClassInfo {
        final String fullClassName;
        final List<String> testMethods = new ArrayList<>();

        TestClassInfo(String fullClassName) {
            this.fullClassName = fullClassName;
        }
    }
}
