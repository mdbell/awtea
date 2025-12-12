package me.mdbell.awtea.util.coverage;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Base class for coverage report generators using the visitor pattern.
 * Subclasses implement specific output formats (HTML, Markdown, etc.)
 */
public abstract class ReportGenerator {
	
	/**
	 * Generate a coverage report from the provided data
	 * @param data Coverage data to generate report from
	 * @param outputPath Path where the report should be written
	 * @throws IOException if an error occurs during generation
	 */
	public abstract void generate(CoverageData data, Path outputPath) throws IOException;
	
	/**
	 * Visit the coverage data root
	 * @param data Coverage data root
	 */
	protected void visitCoverageData(CoverageData data) {
		for (PackageCoverage pkg : data.getPackages().values()) {
			visitPackage(pkg);
		}
	}
	
	/**
	 * Visit a package
	 * @param pkg Package coverage information
	 */
	protected void visitPackage(PackageCoverage pkg) {
		for (ClassCoverage cls : pkg.getClasses()) {
			visitClass(cls);
		}
	}
	
	/**
	 * Visit a class
	 * @param cls Class coverage information
	 */
	protected void visitClass(ClassCoverage cls) {
		// Default implementation does nothing
	}
	
	/**
	 * Format a percentage value
	 * @param percentage Percentage to format
	 * @return Formatted percentage string
	 */
	protected String formatPercentage(double percentage) {
		return String.format("%.1f%%", percentage);
	}
}
