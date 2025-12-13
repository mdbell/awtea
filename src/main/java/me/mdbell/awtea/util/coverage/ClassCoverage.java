package me.mdbell.awtea.util.coverage;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Coverage information for a single class
 */
public class ClassCoverage {
    @Getter
    private final String teavmClassName;
    @Getter
    private final String runtimeClassName;
    @Getter
    private final List<String> implementedMethods = new ArrayList<>();
    @Getter
    private final List<String> missingMethods = new ArrayList<>();
    @Getter
    private final List<String> implementedFields = new ArrayList<>();
    @Getter
    private final List<String> missingFields = new ArrayList<>();
    @Getter
    private final List<String> implementedConstructors = new ArrayList<>();
    @Getter
    private final List<String> missingConstructors = new ArrayList<>();
    private boolean isMissingClass = false;

    public ClassCoverage(String teavmClassName, String runtimeClassName) {
        this.teavmClassName = teavmClassName;
        this.runtimeClassName = runtimeClassName;
    }

    public void setMissingClass(boolean missing) {
        this.isMissingClass = missing;
    }

    public boolean isMissingClass() {
        return isMissingClass;
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

    public void sortMembers() {
        implementedMethods.sort(String::compareTo);
        missingMethods.sort(String::compareTo);
        implementedFields.sort(String::compareTo);
        missingFields.sort(String::compareTo);
        implementedConstructors.sort(String::compareTo);
        missingConstructors.sort(String::compareTo);
    }

    public String getSimpleClassName() {
        int lastDot = runtimeClassName.lastIndexOf('.');
        return lastDot >= 0 ? runtimeClassName.substring(lastDot + 1) : runtimeClassName;
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
