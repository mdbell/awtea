package me.mdbell.awtea.util;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import me.mdbell.awtea.util.coverage.ClassCoverage;
import me.mdbell.awtea.util.coverage.ConsoleReportGenerator;
import me.mdbell.awtea.util.coverage.CoverageData;
import me.mdbell.awtea.util.coverage.HtmlReportGenerator;
import me.mdbell.awtea.util.coverage.MarkdownReportGenerator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ApiDiff {

	private static final String AWTEA_ROOT_PACKAGE = "me.mdbell.awtea.classlib";
	private static final String TEAVM_ROOT_PACKAGE = "org.teavm.classlib";
	private static final String AWTEA_PREFIX = AWTEA_ROOT_PACKAGE + ".";
	private static final String TEAVM_PREFIX = TEAVM_ROOT_PACKAGE + ".";
	private static final String CLASS_PREFIX = "T";

	private static int globalRuntimeTotal = 0;
	private static int globalImplementedTotal = 0;
	private static CoverageData coverageData = null;


	public static void main(String[] args) {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();

		String outputFormat = null;
		String outputPath = null;
		boolean checkMissingClasses = false;
		List<String> packagesToScan = new ArrayList<>();
		List<String> classesToCheckList = new ArrayList<>();
		
		// Parse command-line arguments
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--format") && i + 1 < args.length) {
				outputFormat = args[++i];
			} else if (args[i].equals("--output") && i + 1 < args.length) {
				outputPath = args[++i];
			} else if (args[i].equals("--missing-classes")) {
				checkMissingClasses = true;
			} else if (args[i].equals("--packages") && i + 1 < args.length) {
				String pkgs = args[++i];
				packagesToScan.addAll(Arrays.asList(pkgs.split(",")));
			} else if (args[i].equals("--help") || args[i].equals("-h")) {
				printUsage();
				return;
			} else {
				classesToCheckList.add(args[i]);
			}
		}
		
		// If checking missing classes and no packages specified, default to java.awt.*
		if (checkMissingClasses && packagesToScan.isEmpty()) {
			packagesToScan.add("java.awt");
			packagesToScan.add("java.awt.event");
			packagesToScan.add("java.awt.geom");
			packagesToScan.add("java.awt.image");
		}
		
		if (checkMissingClasses) {
			findMissingClasses(packagesToScan, loader, outputFormat, outputPath);
			return;
		}
		
		String[] classesToCheck = classesToCheckList.isEmpty() ? null : 
			classesToCheckList.toArray(new String[0]);

		// Initialize coverage data if generating a report
		if (outputFormat != null) {
			coverageData = new CoverageData();
			coverageData.setTimestamp(LocalDateTime.now()
				.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		}

		String[] teavmClasses = findClassesInPackage(AWTEA_ROOT_PACKAGE, loader);
		String[] teavmOrigClasses = findClassesInPackage(TEAVM_ROOT_PACKAGE, loader);
		
		// Combine both class lists
		List<String> allClasses = new ArrayList<>();
		allClasses.addAll(Arrays.asList(teavmClasses));
		allClasses.addAll(Arrays.asList(teavmOrigClasses));

		for (String teavmName : allClasses) {
			String runtimeName = mapTeaVmToRuntimeClassName(teavmName);
			if (runtimeName == null) {
				continue;
			};

			if(classesToCheck != null) {
				boolean matched = false;
				for (String cls : classesToCheck) {
					if (teavmName.equals(cls) || runtimeName.equals(cls)) {
						matched = true;
						break;
					}
				}
				if (!matched) {
					continue;
				}
			}

			try {
				Class<?> teavmClass = Class.forName(teavmName, false, loader);
				Class<?> runtimeClass = Class.forName(runtimeName, false, loader);

				compareClasses(teavmClass, runtimeClass);
			} catch (ClassNotFoundException e) {
				// ignored
//				System.out.printf("No runtime class for %s -> %s%n", teavmName, runtimeName);
			} catch (Throwable t) {
				System.out.printf("Error loading %s or %s: %s%n", teavmName, runtimeName, t);
			}
		}

		// Generate report or print to console
		if (outputFormat == null) {
			// Use console generator with visitor pattern
			ConsoleReportGenerator consoleGen = new ConsoleReportGenerator();
			coverageData.accept(consoleGen);
		} else {
			// Generate report using visitor pattern
			generateReport(outputFormat, outputPath);
		}

	}

	private static void printUsage() {
		System.out.println("Usage: ApiDiff [options] [classNames...]");
		System.out.println();
		System.out.println("Options:");
		System.out.println("  --format <html|markdown>  Generate report in specified format");
		System.out.println("  --output <path>           Output file path (default: docs/coverage/report.<ext>)");
		System.out.println("  --missing-classes         Check for missing public classes in packages");
		System.out.println("  --packages <pkg1,pkg2>    Comma-separated list of packages to scan (default: java.awt.*)");
		System.out.println("  --help, -h                Show this help message");
		System.out.println();
		System.out.println("Examples:");
		System.out.println("  ApiDiff                                    # Console output");
		System.out.println("  ApiDiff --format html                      # Generate HTML report");
		System.out.println("  ApiDiff --format markdown --output out.md  # Generate Markdown report");
		System.out.println("  ApiDiff --missing-classes                  # Find missing classes in java.awt.*");
		System.out.println("  ApiDiff --missing-classes --format html    # Missing classes report in docs/coverage/missing/");
		System.out.println("  ApiDiff --missing-classes --packages javax.swing,javax.sound.sampled");
	}

	private static void findMissingClasses(List<String> packagesToScan, ClassLoader loader, String outputFormat, String outputPath) {
		System.out.println("Searching for missing public classes in packages:");
		for (String pkg : packagesToScan) {
			System.out.println("  - " + pkg);
		}
		System.out.println();
		
		Set<String> implementedClasses = new HashSet<>();
		
		// First, collect all classes that awtea implements (both packages)
		String[] teavmClasses = findClassesInPackage(AWTEA_ROOT_PACKAGE, loader);
		String[] teavmOrigClasses = findClassesInPackage(TEAVM_ROOT_PACKAGE, loader);
		
		for (String teavmName : teavmClasses) {
			String runtimeName = mapTeaVmToRuntimeClassName(teavmName);
			if (runtimeName != null) {
				implementedClasses.add(runtimeName);
			}
		}
		
		for (String teavmName : teavmOrigClasses) {
			String runtimeName = mapTeaVmToRuntimeClassName(teavmName);
			if (runtimeName != null) {
				implementedClasses.add(runtimeName);
			}
		}
		
		// Now scan for public classes in runtime JDK
		Map<String, List<String>> missingByPackage = new TreeMap<>();
		int totalMissing = 0;
		int totalFound = 0;
		
		// Create coverage data for missing classes if format is specified
		CoverageData missingCoverageData = null;
		if (outputFormat != null) {
			missingCoverageData = new CoverageData();
			missingCoverageData.setTimestamp(LocalDateTime.now()
				.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		}
		
		for (String pkgToScan : packagesToScan) {
			List<String> missingInPackage = new ArrayList<>();
			
			// For system packages, we need to use rt.jar or system module
			// Try to find classes using Package.getPackage and scanning available classes
			try {
				// Use ClassGraph or manual scanning of rt.jar
				// For simplicity, we'll list known AWT classes
				Set<String> knownClasses = getKnownClassesInPackage(pkgToScan);
				
				for (String className : knownClasses) {
					try {
						Class<?> runtimeClass = Class.forName(className, false, loader);
						
						// Check if it's a public class
						if (Modifier.isPublic(runtimeClass.getModifiers())) {
							totalFound++;
							if (!implementedClasses.contains(className)) {
								missingInPackage.add(className);
								totalMissing++;
								
								// Add to coverage data if generating report
								if (missingCoverageData != null) {
									ClassCoverage classCov = new ClassCoverage(className, className);
									classCov.setMissingClass(true);
									
									// Scan the runtime class to get all its members that are missing
									// Methods
									for (Method m : runtimeClass.getDeclaredMethods()) {
										if (!filterModifiers(m.getModifiers(), runtimeClass.getModifiers())) {
											classCov.addMissingMethod(methodKey(runtimeClass, m));
										}
									}
									
									// Fields
									for (Field f : runtimeClass.getDeclaredFields()) {
										if (!filterModifiers(f.getModifiers(), runtimeClass.getModifiers())) {
											classCov.addMissingField(fieldKey(runtimeClass, f));
										}
									}
									
									// Constructors
									for (Constructor<?> c : runtimeClass.getDeclaredConstructors()) {
										if (!filterModifiers(c.getModifiers(), runtimeClass.getModifiers())) {
											classCov.addMissingConstructor(constructorKey(runtimeClass, c));
										}
									}
									
									missingCoverageData.addClassCoverage(pkgToScan, classCov);
								}
							}
						}
					} catch (ClassNotFoundException e) {
						// Skip classes that can't be loaded
					}
				}
			} catch (Exception e) {
				System.err.println("Error scanning package " + pkgToScan + ": " + e.getMessage());
			}
			
			if (!missingInPackage.isEmpty()) {
				Collections.sort(missingInPackage);
				missingByPackage.put(pkgToScan, missingInPackage);
			}
		}
		
		// Print results
		System.out.println("======================================================");
		System.out.println("Missing Classes Report");
		System.out.println("======================================================");
		System.out.printf("Total public classes found: %d%n", totalFound);
		System.out.printf("Total missing classes: %d%n", totalMissing);
		System.out.printf("Coverage: %d / %d (%.1f%%)%n", 
			totalFound - totalMissing, totalFound, 
			totalFound == 0 ? 100.0 : (100.0 * (totalFound - totalMissing) / totalFound));
		System.out.println("======================================================");
		System.out.println();
		
		if (totalMissing > 0) {
			for (Map.Entry<String, List<String>> entry : missingByPackage.entrySet()) {
				String pkg = entry.getKey();
				List<String> missing = entry.getValue();
				
				System.out.printf("Package: %s (%d missing)%n", pkg, missing.size());
				for (String className : missing) {
					System.out.println("  - " + className);
				}
				System.out.println();
			}
			
			// Generate reports if format specified
			if (outputFormat != null && missingCoverageData != null) {
				generateMissingClassReports(outputFormat, outputPath, missingCoverageData);
			}
		} else {
			System.out.println("✓ All public classes are implemented!");
		}
	}
	
	private static Set<String> getKnownClassesInPackage(String pkgName) {
		Set<String> classes = new HashSet<>();
		
		try (ScanResult scanResult = new ClassGraph()
				.enableSystemJarsAndModules()  // Scan system JARs and modules
				.acceptPackages(pkgName)        // Only scan the specified package
				.scan()) {
			
			for (ClassInfo classInfo : scanResult.getAllClasses()) {
				// Only include public classes directly in this package (not subpackages)
				if (classInfo.isPublic() && classInfo.getPackageName().equals(pkgName)) {
					classes.add(classInfo.getName());
				}
			}
		} catch (Exception e) {
			System.err.println("Error: Failed to scan for classes in " + pkgName + ": " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		
		return classes;
	}
	
	private static void generateReport(String format, String outputPath) {
		Path path;
		if (outputPath == null) {
			String ext = format.equals("html") ? "html" : "md";
			path = Paths.get("docs/coverage/report." + ext);
		} else {
			path = Paths.get(outputPath);
		}
		
		if (format.equalsIgnoreCase("html")) {
			HtmlReportGenerator gen = new HtmlReportGenerator(path);
			coverageData.accept(gen);
			System.out.println("HTML report generated: " + path.toAbsolutePath());
		} else if (format.equalsIgnoreCase("markdown")) {
			MarkdownReportGenerator gen = new MarkdownReportGenerator(path);
			coverageData.accept(gen);
			System.out.println("Markdown report generated: " + path.toAbsolutePath());
		} else {
			System.err.println("Unknown format: " + format);
			System.err.println("Supported formats: html, markdown");
		}
	}
	
	private static void generateMissingClassReports(String format, String outputPath, CoverageData missingData) {
		Path path;
		if (outputPath == null) {
			String ext = format.equals("html") ? "html" : "md";
			path = Paths.get("docs/coverage/missing/report." + ext);
		} else {
			// Place in missing subdirectory
			Path originalPath = Paths.get(outputPath);
			Path parent = originalPath.getParent();
			String fileName = originalPath.getFileName().toString();
			path = (parent != null ? parent : Paths.get(".")).resolve("missing").resolve(fileName);
		}
		
		if (format.equalsIgnoreCase("html")) {
			HtmlReportGenerator gen = new HtmlReportGenerator(path);
			missingData.accept(gen);
			System.out.println("Missing classes HTML report generated: " + path.toAbsolutePath());
		} else if (format.equalsIgnoreCase("markdown")) {
			MarkdownReportGenerator gen = new MarkdownReportGenerator(path);
			missingData.accept(gen);
			System.out.println("Missing classes Markdown report generated: " + path.toAbsolutePath());
		} else {
			System.err.println("Unknown format: " + format);
			System.err.println("Supported formats: html, markdown");
		}
	}


	private static String[] findClassesInPackage(String pkg, ClassLoader loader) {
		Set<String> classes = new HashSet<>();
		
		try (ScanResult scanResult = new ClassGraph()
				.overrideClassLoaders(loader)
				.acceptPackages(pkg)
				.scan()) {
			
			for (ClassInfo classInfo : scanResult.getAllClasses()) {
				classes.add(classInfo.getName());
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to scan for classes in package: " + pkg, e);
		}
		
		String[] array = new String[classes.size()];
		classes.toArray(array);
		Arrays.sort(array);
		return array;
	}

	private static String mapTeaVmToRuntimeClassName(String teavmName) {
		String withoutPrefix = null;
		
		// Check if it's from the awtea package
		if (teavmName.startsWith(AWTEA_PREFIX)) {
			withoutPrefix = teavmName.substring(AWTEA_PREFIX.length());
		}
		// Check if it's from the org.teavm.classlib package
		else if (teavmName.startsWith(TEAVM_PREFIX)) {
			withoutPrefix = teavmName.substring(TEAVM_PREFIX.length());
		}
		// Not from either package
		else {
			return null;
		}

		int lastDot = withoutPrefix.lastIndexOf('.');
		String pkg = (lastDot == -1) ? "" : withoutPrefix.substring(0, lastDot);
		String simple = (lastDot == -1) ? withoutPrefix : withoutPrefix.substring(lastDot + 1);

		// Handle nested types: TOuter$Inner
		int dollar = simple.indexOf('$');
		String outer = (dollar == -1) ? simple : simple.substring(0, dollar);
		String rest = (dollar == -1) ? "" : simple.substring(dollar);

		if (outer.startsWith(CLASS_PREFIX) &&
			outer.length() > 1 &&
			Character.isUpperCase(outer.charAt(1))) {
			outer = outer.substring(1);
		}

		return pkg.isEmpty()
			? outer + rest
			: pkg + "." + outer + rest;
	}

	private static String normalizeTypeForApi(Class<?> type) {
		if (type.isArray()) {
			return normalizeTypeForApi(type.getComponentType()) + "[]";
		}

		String name = type.getTypeName();

		if (type.isPrimitive()) {
			return type.getName();
		}

		String teavmName = mapTeaVmToRuntimeClassName(name);
		if (teavmName != null) {
			return teavmName;
		}

		return name;
	}

	private static void compareClasses(Class<?> teavmClass, Class<?> runtimeClass) {
		Set<String> runtimeMethods = describeMethods(runtimeClass);
		Set<String> runtimeFields  = describeFields(runtimeClass);
		Set<String> runtimeCtors   = describeCtors(runtimeClass);

		Set<String> teavmMethods = describeMethods(teavmClass);
		Set<String> teavmFields  = describeFields(teavmClass);
		Set<String> teavmCtors   = describeCtors(teavmClass);

		// Runtime API items that TeaVM *did not* implement
		Set<String> missingMethods = diff(runtimeMethods, teavmMethods);
		Set<String> missingFields  = diff(runtimeFields,  teavmFields);
		Set<String> missingCtors   = diff(runtimeCtors,   teavmCtors);
		
		// Implemented items
		Set<String> implementedMethods = diff(runtimeMethods, missingMethods);
		Set<String> implementedFields  = diff(runtimeFields,  missingFields);
		Set<String> implementedCtors   = diff(runtimeCtors,   missingCtors);

		// Totals
		int runtimeTotal = runtimeMethods.size() + runtimeFields.size() + runtimeCtors.size();
		int implementedTotal = runtimeTotal
			- (missingMethods.size() + missingFields.size() + missingCtors.size());

		// Update global totals
		globalRuntimeTotal += runtimeTotal;
		globalImplementedTotal += implementedTotal;

		// If generating coverage data, populate it
		if (coverageData != null) {
			ClassCoverage classCoverage = new ClassCoverage(
				teavmClass.getName(), 
				runtimeClass.getName()
			);
			
			implementedMethods.forEach(classCoverage::addImplementedMethod);
			missingMethods.forEach(classCoverage::addMissingMethod);
			implementedFields.forEach(classCoverage::addImplementedField);
			missingFields.forEach(classCoverage::addMissingField);
			implementedCtors.forEach(classCoverage::addImplementedConstructor);
			missingCtors.forEach(classCoverage::addMissingConstructor);
			
			String packageName = runtimeClass.getPackage() != null ? 
				runtimeClass.getPackage().getName() : "(default)";
			coverageData.addClassCoverage(packageName, classCoverage);
		}

		// Print console output only if not generating a report
		if (coverageData == null) {
			// If class fully covered, you can skip printing
			if (missingMethods.isEmpty() && missingFields.isEmpty() && missingCtors.isEmpty()) {
				System.out.printf("=== %s: FULL COVERAGE (%d/%d = 100%%)%n",
					runtimeClass.getName(), implementedTotal, runtimeTotal);
				return;
			}

			// Print diff
			System.out.printf("=== %s vs %s ===%n", teavmClass.getName(), runtimeClass.getName());
			System.out.printf("Coverage: %d/%d = %.1f%%%n",
				implementedTotal, runtimeTotal,
				(runtimeTotal == 0 ? 100.0 : (100.0 * implementedTotal / runtimeTotal)));

			if (!missingMethods.isEmpty()) {
				System.out.println("  Missing methods:");
				missingMethods.forEach(m -> System.out.println("    " + m));
			}
			if (!missingFields.isEmpty()) {
				System.out.println("  Missing fields:");
				missingFields.forEach(f -> System.out.println("    " + f));
			}
			if (!missingCtors.isEmpty()) {
				System.out.println("  Missing ctors:");
				missingCtors.forEach(c -> System.out.println("    " + c));
			}

			System.out.println();
		}
	}


	private static String modifiersFor(int mods) {
		StringBuilder sb = new StringBuilder();
		if (Modifier.isPublic(mods)){
			sb.append("public ");
		}
		else if (Modifier.isProtected(mods)){
			sb.append("protected ");
		}
		else if (Modifier.isPrivate(mods)){
			sb.append("private ");
		}

		if (Modifier.isStatic(mods)){
			sb.append("static ");
		}
		if (Modifier.isAbstract(mods)){
			sb.append("abstract ");
		}
		if (Modifier.isFinal(mods)){
			sb.append("final ");
		}

		// trim trailing space
		int len = sb.length();
		if (len > 0 && sb.charAt(len - 1) == ' ') {
			sb.setLength(len - 1);
		}
		return sb.toString();
	}

	private static boolean filterModifiers(int mods, int clazzModifiers) {
		return !Modifier.isPublic(mods) && !(
			// we only care about protected members in non-final classes, in case
			// a user-defined subclass wants to override them
			Modifier.isProtected(mods) && !Modifier.isFinal(clazzModifiers)
			);
	}


	private static Set<String> describeMethods(Class<?> c) {
		Method[] methods = Arrays.stream(c.getDeclaredMethods()).filter(
			m -> m.getDeclaringClass() == c
		).toArray(Method[]::new);
		Set<String> out = new HashSet<>();
		for (Method m : methods) {
			if (filterModifiers(m.getModifiers(), c.getModifiers())) {
				continue;
			}
			out.add(methodKey(c, m));
		}
		return out;
	}

	private static Set<String> describeFields(Class<?> c) {
		Set<String> out = new HashSet<>();
		Field[] fields = Arrays.stream(c.getDeclaredFields()).filter(
			f -> f.getDeclaringClass() == c
		).toArray(Field[]::new);
		for (Field f : fields) {
			if (filterModifiers(f.getModifiers(), c.getModifiers())) {
				continue;
			}
			out.add(fieldKey(c, f));
		}
		return out;
	}

	private static Set<String> describeCtors(Class<?> c) {
		Set<String> out = new HashSet<>();
		Constructor<?>[] ctors = Arrays.stream(c.getDeclaredConstructors()).filter(
			ctor -> ctor.getDeclaringClass() == c
		).toArray(Constructor<?>[]::new);
		for (Constructor<?> ctor : ctors) {
			if (filterModifiers(ctor.getModifiers(), c.getModifiers())) {
				continue;
			}
			out.add(constructorKey(c, ctor));
		}
		return out;
	}

	private static String methodKey(Class<?> owner, Method m) {
		StringBuilder sb = new StringBuilder(modifiersFor(m.getModifiers()));
		sb.append(" ")
			.append(normalizeTypeForApi(m.getReturnType()))
			.append(" ")
			.append(m.getName()).append("(");
		Class<?>[] params = m.getParameterTypes();
		for (int i = 0; i < params.length; i++) {
			if (i > 0) sb.append(", ");
			sb.append(normalizeTypeForApi(params[i]));
		}
		sb.append(")");
		return sb.toString();
	}

	private static String constructorKey(Class<?> owner, Constructor<?> c) {
		StringBuilder sb = new StringBuilder(modifiersFor(c.getModifiers()));
		sb.append(" ")
			.append(normalizeTypeForApi(owner))
			.append("(");
		Class<?>[] params = c.getParameterTypes();
		for (int i = 0; i < params.length; i++) {
			if (i > 0) sb.append(", ");
			sb.append(normalizeTypeForApi(params[i]));
		}
		sb.append(")");
		return sb.toString();
	}

	private static String fieldKey(Class<?> owner, Field f) {
		return modifiersFor(f.getModifiers()) + " "
			+ normalizeTypeForApi(f.getType()) +
			" " +
			f.getName();
	}

	private static Set<String> diff(Set<String> a, Set<String> b) {
		Set<String> result = new TreeSet<>(a);
		result.removeAll(b);
		return result;
	}
}
