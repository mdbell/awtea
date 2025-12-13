package me.mdbell.awtea.util.coverage;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.io.IOException;

/**
 * Generates console output for coverage reports
 */
public class ConsoleReportGenerator extends ReportGenerator {

	private static final Logger log = LoggerFactory.getLogger(ConsoleReportGenerator.class);
	
	public ConsoleReportGenerator() {
		super(null);
	}

	@Override
	protected void generate(CoverageData data) throws IOException {
		log.info("======================================================");
		log.info("Global API Coverage Summary:");
		log.info("Total covered: %d / %d (%.1f%%)",
			data.getTotalImplemented(),
			data.getTotalRuntime(),
			data.getTotalPercentage());
		log.info("======================================================");
	}
}
