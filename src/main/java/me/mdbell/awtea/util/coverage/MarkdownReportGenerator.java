package me.mdbell.awtea.util.coverage;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Generates Markdown coverage reports split into multiple files in a directory structure.
 * Creates an index file and separate files per package and class organized by package hierarchy.
 */
public class MarkdownReportGenerator extends ReportGenerator {

    private Path mdDir;

    public MarkdownReportGenerator(Path outputPath) {
        super(outputPath);
    }

    @Override
    protected void generate(CoverageData data) throws IOException {

        // outputPath is the root index file path
        // We'll use its parent as the output directory
        Path outputDir;
        if (outputPath.getParent() != null) {
            outputDir = outputPath.getParent();
            Files.createDirectories(outputDir);
        } else {
            outputDir = Path.of(".");
        }

        // Create md subdirectory for organized structure
        this.mdDir = outputDir.resolve("md");
        Files.createDirectories(mdDir);

        // Generate index file
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputPath))) {
            writeIndex(data, writer);
        }

        // Generate package index files and class files
        for (Map.Entry<String, PackageCoverage> entry : data.getPackages().entrySet()) {
            String pkgName = entry.getKey();
            PackageCoverage pkg = entry.getValue();
            generatePackageFiles(pkgName, pkg);
        }
    }

    private void writeIndex(CoverageData data, PrintWriter out) {
        out.println("# AWT API Coverage Report");
        out.println();

        if (data.getTimestamp() != null) {
            out.printf("**Generated:** %s%n", data.getTimestamp());
            out.println();
        }

        writeSummary(data, out);
        writePackageTable(data, out);
    }

    private void writeSummary(CoverageData data, PrintWriter out) {
        out.println("## Summary");
        out.println();
        out.printf("- **Total Coverage**: %d / %d (%.1f%%)%n",
                data.getTotalImplemented(), data.getTotalRuntime(), data.getTotalPercentage());
        out.printf("- **Packages**: %d%n", data.getPackages().size());

        int totalClasses = data.getPackages().values().stream()
                .mapToInt(pkg -> pkg.getClasses().size())
                .sum();
        out.printf("- **Classes**: %d%n", totalClasses);
        out.println();

        writeProgressBar(out, data.getTotalPercentage());
        out.println();
    }

    private void writePackageTable(CoverageData data, PrintWriter out) {
        out.println("## Packages");
        out.println();
        out.println("| Package | Classes | Coverage | Percentage |");
        out.println("|---------|---------|----------|------------|");

        for (Map.Entry<String, PackageCoverage> entry : data.getPackages().entrySet()) {
            String pkgName = entry.getKey();
            PackageCoverage pkg = entry.getValue();

            String pkgIndexPath = getPackageIndexPath(pkgName);
            String badge = getCoverageBadge(pkg.getPercentage());

            out.printf("| [`%s`](%s) | %d | %d / %d | %s %s |%n",
                    pkgName,
                    pkgIndexPath,
                    pkg.getClasses().size(),
                    pkg.getImplementedCount(),
                    pkg.getTotalCount(),
                    formatPercentage(pkg.getPercentage()),
                    badge);
        }

        out.println();
        out.println("Click on a package name to see detailed coverage information for that package and its classes.");
        out.println();
    }

    private void generatePackageFiles(String pkgName, PackageCoverage pkg) throws IOException {
// Create package directory structure (e.g., md/java/awt/)
        Path pkgDir = getPackageDirectory(pkgName);
        Files.createDirectories(pkgDir);

// Generate package index file
        Path pkgIndexFile = pkgDir.resolve("index.md");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(pkgIndexFile))) {
            writePackageIndex(pkgName, pkg, writer);
        }

// Generate individual class files
        for (ClassCoverage cls : pkg.getClasses()) {
            generateClassFile(pkgName, cls, pkgDir);
        }
    }

    private void writePackageIndex(String pkgName, PackageCoverage pkg, PrintWriter out) {
        out.printf("# Package: `%s`%n", pkgName);
        out.println();
        out.printf("**Coverage:** %d / %d (%.1f%%)%n",
                pkg.getImplementedCount(), pkg.getTotalCount(), pkg.getPercentage());
        out.println();
        writeProgressBar(out, pkg.getPercentage());
        out.println();

        out.println("## Classes");
        out.println();

        for (ClassCoverage cls : pkg.getClasses()) {
            String classFilePath = getClassFileName(cls.getSimpleClassName());
            out.printf("### [`%s`](%s) %s%n", cls.getSimpleClassName(), classFilePath, getCoverageBadge(cls.getPercentage()));
            out.println();
            out.printf("**Coverage:** %d / %d (%.1f%%)%n",
                    cls.getImplementedCount(), cls.getTotalCount(), cls.getPercentage());
            out.println();
            writeProgressBar(out, cls.getPercentage());
            out.println();
        }

        out.println();
// Calculate relative path back to report.md
        int depth = pkgName.split("\\.").length;
        StringBuilder backPath = new StringBuilder();
        for (int i = 0; i < depth + 1; i++) {
            backPath.append("../");
        }
        backPath.append("../report.md");
        out.printf("[← Back to Index](%s)%n", backPath.toString());
    }

    private void generateClassFile(String pkgName, ClassCoverage cls, Path pkgDir) throws IOException {
        String classFileName = getClassFileName(cls.getSimpleClassName());
        Path classFile = pkgDir.resolve(classFileName);

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(classFile))) {
            writeClass(pkgName, cls, writer);
        }
    }

    private void writeClass(String pkgName, ClassCoverage cls, PrintWriter out) {
        out.printf("# Class: `%s` %s%n", cls.getSimpleClassName(), getCoverageBadge(cls.getPercentage()));
        out.println();
        out.printf("**Full Name:** `%s`%n", cls.getRuntimeClassName());
        out.println();
        out.printf("**Coverage:** %d / %d (%.1f%%)%n",
                cls.getImplementedCount(), cls.getTotalCount(), cls.getPercentage());
        out.println();
        writeProgressBar(out, cls.getPercentage());
        out.println();

        if (!cls.getImplementedMethods().isEmpty()) {
            out.println("## ✓ Implemented Methods");
            out.println();
            for (String method : cls.getImplementedMethods()) {
                out.printf("- `%s`%n", method);
            }
            out.println();
        }

        if (!cls.getMissingMethods().isEmpty()) {
            out.println("## ✗ Missing Methods");
            out.println();
            for (String method : cls.getMissingMethods()) {
                out.printf("- `%s`%n", method);
            }
            out.println();
        }

        if (!cls.getImplementedFields().isEmpty()) {
            out.println("## ✓ Implemented Fields");
            out.println();
            for (String field : cls.getImplementedFields()) {
                out.printf("- `%s`%n", field);
            }
            out.println();
        }

        if (!cls.getMissingFields().isEmpty()) {
            out.println("## ✗ Missing Fields");
            out.println();
            for (String field : cls.getMissingFields()) {
                out.printf("- `%s`%n", field);
            }
            out.println();
        }

        if (!cls.getImplementedConstructors().isEmpty()) {
            out.println("## ✓ Implemented Constructors");
            out.println();
            for (String ctor : cls.getImplementedConstructors()) {
                out.printf("- `%s`%n", ctor);
            }
            out.println();
        }

        if (!cls.getMissingConstructors().isEmpty()) {
            out.println("## ✗ Missing Constructors");
            out.println();
            for (String ctor : cls.getMissingConstructors()) {
                out.printf("- `%s`%n", ctor);
            }
            out.println();
        }

        out.println();
        out.println("[← Back to Package](index.md)");
    }

    private void writeProgressBar(PrintWriter out, double percentage) {
        int barWidth = 50;
        int filled = (int) (barWidth * percentage / 100.0);
        int empty = barWidth - filled;

        out.print("```");
        out.println();
        out.print("[");
        for (int i = 0; i < filled; i++) {
            out.print("█");
        }
        for (int i = 0; i < empty; i++) {
            out.print("░");
        }
        out.printf("] %.1f%%%n", percentage);
        out.print("```");
        out.println();
    }

    private String getCoverageBadge(double percentage) {
        if (percentage == 100) {
            return "![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)";
        } else if (percentage >= 75) {
            return String.format("![Coverage](https://img.shields.io/badge/coverage-%.1f%%25-green)", percentage);
        } else if (percentage >= 50) {
            return String.format("![Coverage](https://img.shields.io/badge/coverage-%.1f%%25-yellow)", percentage);
        } else if (percentage >= 25) {
            return String.format("![Coverage](https://img.shields.io/badge/coverage-%.1f%%25-orange)", percentage);
        } else {
            return String.format("![Coverage](https://img.shields.io/badge/coverage-%.1f%%25-red)", percentage);
        }
    }

    /**
     * Get the directory path for a package (e.g., "java.awt" -> "md/java/awt")
     */
    private Path getPackageDirectory(String pkgName) {
        String[] parts = pkgName.split("\\.");
        Path dir = mdDir;
        for (String part : parts) {
            dir = dir.resolve(part);
        }
        return dir;
    }

    /**
     * Get the relative path from report.md to package index (e.g., "md/java/awt/index.md")
     */
    private String getPackageIndexPath(String pkgName) {
        String[] parts = pkgName.split("\\.");
        StringBuilder path = new StringBuilder("md");
        for (String part : parts) {
            path.append("/").append(part);
        }
        path.append("/index.md");
        return path.toString();
    }

    /**
     * Get the filename for a class (e.g., "Container" -> "Container.md")
     */
    private String getClassFileName(String className) {
// Handle nested classes (e.g., "Outer$Inner" -> "Outer_Inner.md")
        return className.replace('$', '_') + ".md";
    }
}
