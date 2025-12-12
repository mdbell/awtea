package me.mdbell.awtea.util.coverage;

import java.util.ArrayList;
import java.util.List;

/**
 * Coverage information for a single class
 */
public class ClassCoverage {
	private final String teavmClassName;
	private final String runtimeClassName;
	private final List<String> implementedMethods = new ArrayList<>();
	private final List<String> missingMethods = new ArrayList<>();
	private final List<String> implementedFields = new ArrayList<>();
	private final List<String> missingFields = new ArrayList<>();
	private final List<String> implementedConstructors = new ArrayList<>();
	private final List<String> missingConstructors = new ArrayList<>();

	public ClassCoverage(String teavmClassName, String runtimeClassName) {
		this.teavmClassName = teavmClassName;
		this.runtimeClassName = runtimeClassName;
	}

	public void addImplementedMethod(String method) {
		implementedMethods.add(method);
	}

	public void addMissingMethod(String method) {
		missingMethods.add(method);
	}

	public void addImplementedField(String field) {
		implementedFields.add(field);
	}

	public void addMissingField(String field) {
		missingFields.add(field);
	}

	public void addImplementedConstructor(String constructor) {
		implementedConstructors.add(constructor);
	}

	public void addMissingConstructor(String constructor) {
		missingConstructors.add(constructor);
	}

	public String getTeavmClassName() {
		return teavmClassName;
	}

	public String getRuntimeClassName() {
		return runtimeClassName;
	}

	public String getSimpleClassName() {
		int lastDot = runtimeClassName.lastIndexOf('.');
		return lastDot >= 0 ? runtimeClassName.substring(lastDot + 1) : runtimeClassName;
	}

	public List<String> getImplementedMethods() {
		return implementedMethods;
	}

	public List<String> getMissingMethods() {
		return missingMethods;
	}

	public List<String> getImplementedFields() {
		return implementedFields;
	}

	public List<String> getMissingFields() {
		return missingFields;
	}

	public List<String> getImplementedConstructors() {
		return implementedConstructors;
	}

	public List<String> getMissingConstructors() {
		return missingConstructors;
	}

	public int getImplementedCount() {
		return implementedMethods.size() + implementedFields.size() + implementedConstructors.size();
	}

	public int getTotalCount() {
		return implementedMethods.size() + missingMethods.size()
			+ implementedFields.size() + missingFields.size()
			+ implementedConstructors.size() + missingConstructors.size();
	}

	public double getPercentage() {
		int total = getTotalCount();
		return total == 0 ? 100.0 : (100.0 * getImplementedCount() / total);
	}

	public boolean isFullyCovered() {
		return missingMethods.isEmpty() && missingFields.isEmpty() && missingConstructors.isEmpty();
	}
}
