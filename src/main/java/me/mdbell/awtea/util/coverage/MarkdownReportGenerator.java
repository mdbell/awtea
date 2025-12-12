package me.mdbell.awtea.util.coverage;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Generates Markdown coverage reports
 */
public class MarkdownReportGenerator {

	public void generate(CoverageData data, Path outputPath) throws IOException {
		if (outputPath.getParent() != null) {
			Files.createDirectories(outputPath.getParent());
		}
		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputPath))) {
			writeMarkdown(data, writer);
		}
	}

	private void writeMarkdown(CoverageData data, PrintWriter out) {
		out.println("# AWT API Coverage Report");
		out.println();
		
		if (data.getTimestamp() != null) {
			out.printf("**Generated:** %s%n", data.getTimestamp());
			out.println();
		}
		
		writeSummary(data, out);
		writePackages(data, out);
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

	private void writePackages(CoverageData data, PrintWriter out) {
		for (Map.Entry<String, PackageCoverage> entry : data.getPackages().entrySet()) {
			String pkgName = entry.getKey();
			PackageCoverage pkg = entry.getValue();
			
			out.printf("## Package: `%s`%n", pkgName);
			out.println();
			out.printf("**Coverage:** %d / %d (%.1f%%)%n",
				pkg.getImplementedCount(), pkg.getTotalCount(), pkg.getPercentage());
			out.println();
			writeProgressBar(out, pkg.getPercentage());
			out.println();
			
			for (ClassCoverage cls : pkg.getClasses()) {
				writeClass(cls, out);
			}
		}
	}

	private void writeClass(ClassCoverage cls, PrintWriter out) {
		out.printf("### Class: `%s` %s%n", cls.getSimpleClassName(), getCoverageBadge(cls.getPercentage()));
		out.println();
		out.printf("**Coverage:** %d / %d (%.1f%%)%n",
			cls.getImplementedCount(), cls.getTotalCount(), cls.getPercentage());
		out.println();
		writeProgressBar(out, cls.getPercentage());
		out.println();
		
		if (!cls.getImplementedMethods().isEmpty()) {
			out.println("#### ✓ Implemented Methods");
			out.println();
			for (String method : cls.getImplementedMethods()) {
				out.printf("- `%s`%n", method);
			}
			out.println();
		}
		
		if (!cls.getMissingMethods().isEmpty()) {
			out.println("#### ✗ Missing Methods");
			out.println();
			for (String method : cls.getMissingMethods()) {
				out.printf("- `%s`%n", method);
			}
			out.println();
		}
		
		if (!cls.getImplementedFields().isEmpty()) {
			out.println("#### ✓ Implemented Fields");
			out.println();
			for (String field : cls.getImplementedFields()) {
				out.printf("- `%s`%n", field);
			}
			out.println();
		}
		
		if (!cls.getMissingFields().isEmpty()) {
			out.println("#### ✗ Missing Fields");
			out.println();
			for (String field : cls.getMissingFields()) {
				out.printf("- `%s`%n", field);
			}
			out.println();
		}
		
		if (!cls.getImplementedConstructors().isEmpty()) {
			out.println("#### ✓ Implemented Constructors");
			out.println();
			for (String ctor : cls.getImplementedConstructors()) {
				out.printf("- `%s`%n", ctor);
			}
			out.println();
		}
		
		if (!cls.getMissingConstructors().isEmpty()) {
			out.println("#### ✗ Missing Constructors");
			out.println();
			for (String ctor : cls.getMissingConstructors()) {
				out.printf("- `%s`%n", ctor);
			}
			out.println();
		}
		
		out.println("---");
		out.println();
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
}
