package me.mdbell.awtea.util.coverage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Root container for API coverage data
 */
public class CoverageData {
	private final Map<String, PackageCoverage> packages = new TreeMap<>();
	private int totalImplemented = 0;
	private int totalRuntime = 0;
	private String timestamp;

	public void addClassCoverage(String packageName, ClassCoverage classCoverage) {
		PackageCoverage pkg = packages.computeIfAbsent(packageName, PackageCoverage::new);
		pkg.addClass(classCoverage);
		totalImplemented += classCoverage.getImplementedCount();
		totalRuntime += classCoverage.getTotalCount();
	}

	public Map<String, PackageCoverage> getPackages() {
		return packages;
	}

	public int getTotalImplemented() {
		return totalImplemented;
	}

	public int getTotalRuntime() {
		return totalRuntime;
	}

	public double getTotalPercentage() {
		return totalRuntime == 0 ? 100.0 : (100.0 * totalImplemented / totalRuntime);
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
}
