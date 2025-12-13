package me.mdbell.awtea.util.coverage;

import java.io.IOException;

/**
 * Generates console output for coverage reports
 */
public class ConsoleReportGenerator extends ReportGenerator {
	
	public ConsoleReportGenerator() {
		super(null);
	}

	@Override
	protected void generate(CoverageData data) throws IOException {
		System.out.println("======================================================");
		System.out.println("Global API Coverage Summary:");
		System.out.printf("Total covered: %d / %d (%.1f%%)%n",
			data.getTotalImplemented(),
			data.getTotalRuntime(),
			data.getTotalPercentage());
		System.out.println("======================================================");
	}
}
