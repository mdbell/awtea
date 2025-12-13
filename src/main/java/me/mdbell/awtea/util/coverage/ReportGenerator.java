package me.mdbell.awtea.util.coverage;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Base class for coverage report generators using the visitor pattern.
 * Subclasses implement specific output formats (HTML, Markdown, Console, etc.)
 */
public abstract class ReportGenerator {
	
	protected Path outputPath;
	
	/**
	 * Constructor with output path
	 * @param outputPath Path where the report should be written (null for console output)
	 */
	public ReportGenerator(Path outputPath) {
		this.outputPath = outputPath;
	}
	
	/**
	 * Visit and process the coverage data
	 * @param data Coverage data to process
	 */
	public void visit(CoverageData data) {
		try {
			generate(data);
		} catch (IOException e) {
			throw new RuntimeException("Failed to generate report: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Generate the report from coverage data
	 * @param data Coverage data to generate report from
	 * @throws IOException if an error occurs during generation
	 */
	protected abstract void generate(CoverageData data) throws IOException;
	
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
