package me.mdbell.awtea.util.coverage;

import java.util.ArrayList;
import java.util.List;

/**
 * Coverage information for a Java package
 */
public class PackageCoverage {
	private final String name;
	private final List<ClassCoverage> classes = new ArrayList<>();
	private int implementedCount = 0;
	private int totalCount = 0;

	public PackageCoverage(String name) {
		this.name = name;
	}

	public void addClass(ClassCoverage classCoverage) {
		classes.add(classCoverage);
		implementedCount += classCoverage.getImplementedCount();
		totalCount += classCoverage.getTotalCount();
	}

	public String getName() {
		return name;
	}

	public List<ClassCoverage> getClasses() {
		return classes;
	}

	public int getImplementedCount() {
		return implementedCount;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public double getPercentage() {
		return totalCount == 0 ? 100.0 : (100.0 * implementedCount / totalCount);
	}
}
