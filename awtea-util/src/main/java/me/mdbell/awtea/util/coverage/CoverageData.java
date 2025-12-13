package me.mdbell.awtea.util.coverage;

import lombok.Setter;

import java.util.Map;
import java.util.TreeMap;

/**
 * Root container for API coverage data
 */
public class CoverageData {
    private final Map<String, PackageCoverage> packages = new TreeMap<>();
    private int totalImplemented = 0;
    private int totalRuntime = 0;
    @Setter
    private String timestamp;

    public void addClassCoverage(String packageName, ClassCoverage classCoverage) {
        PackageCoverage pkg = packages.computeIfAbsent(packageName, PackageCoverage::new);
        pkg.addClass(classCoverage);
        totalImplemented += classCoverage.getImplementedCount();
        totalRuntime += classCoverage.getTotalCount();
    }

    public void sortPackages() {
        packages.values().forEach(PackageCoverage::sortMembers);
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

    /**
     * Accept a visitor to process this coverage data
     *
     * @param visitor The report generator visitor
     */
    public void accept(ReportGenerator visitor) {
        visitor.visit(this);
    }

    public void finalizeData() {
        sortPackages();
    }
}
