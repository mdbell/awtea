package me.mdbell.awtea.util;

import me.mdbell.awtea.util.coverage.ClassCoverage;
import me.mdbell.awtea.util.coverage.CoverageData;
import me.mdbell.awtea.util.coverage.HtmlReportGenerator;
import me.mdbell.awtea.util.coverage.MarkdownReportGenerator;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ApiDiff {

	private static final String AWTEA_ROOT_PACKAGE = "me.mdbell.awtea.classlib";
	private static final String AWTEA_PREFIX = AWTEA_ROOT_PACKAGE + ".";
	private static final String CLASS_PREFIX = "T";

	private static int globalRuntimeTotal = 0;
	private static int globalImplementedTotal = 0;
	private static CoverageData coverageData = null;


	public static void main(String[] args) {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();

		String outputFormat = null;
		String outputPath = null;
		List<String> classesToCheckList = new ArrayList<>();
		
		// Parse command-line arguments
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--format") && i + 1 < args.length) {
				outputFormat = args[++i];
			} else if (args[i].equals("--output") && i + 1 < args.length) {
				outputPath = args[++i];
			} else if (args[i].equals("--help") || args[i].equals("-h")) {
				printUsage();
				return;
			} else {
				classesToCheckList.add(args[i]);
			}
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

		for (String teavmName : teavmClasses) {
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

		// Print console summary if not generating a report
		if (outputFormat == null) {
			System.out.println("======================================================");
			System.out.println("Global API Coverage Summary:");
			System.out.printf("Total covered: %d / %d (%.1f%%)%n",
				globalImplementedTotal,
				globalRuntimeTotal,
				globalRuntimeTotal == 0 ? 100.0 : (100.0 * globalImplementedTotal / globalRuntimeTotal));
			System.out.println("======================================================");
		} else {
			// Generate report
			generateReport(outputFormat, outputPath);
		}

	}

	private static void printUsage() {
		System.out.println("Usage: ApiDiff [options] [classNames...]");
		System.out.println();
		System.out.println("Options:");
		System.out.println("  --format <html|markdown>  Generate report in specified format");
		System.out.println("  --output <path>           Output file path (default: docs/coverage/report.<ext>)");
		System.out.println("  --help, -h                Show this help message");
		System.out.println();
		System.out.println("Examples:");
		System.out.println("  ApiDiff                                    # Console output");
		System.out.println("  ApiDiff --format html                      # Generate HTML report");
		System.out.println("  ApiDiff --format markdown --output out.md  # Generate Markdown report");
	}

	private static void generateReport(String format, String outputPath) {
		try {
			Path path;
			if (outputPath == null) {
				String ext = format.equals("html") ? "html" : "md";
				path = Paths.get("docs/coverage/report." + ext);
			} else {
				path = Paths.get(outputPath);
			}
			
			if (format.equalsIgnoreCase("html")) {
				new HtmlReportGenerator().generate(coverageData, path);
				System.out.println("HTML report generated: " + path.toAbsolutePath());
			} else if (format.equalsIgnoreCase("markdown")) {
				new MarkdownReportGenerator().generate(coverageData, path);
				System.out.println("Markdown report generated: " + path.toAbsolutePath());
			} else {
				System.err.println("Unknown format: " + format);
				System.err.println("Supported formats: html, markdown");
			}
		} catch (IOException e) {
			System.err.println("Failed to generate report: " + e.getMessage());
			e.printStackTrace();
		}
	}


	private static String[] findClassesInPackage(String pkg, ClassLoader loader) {
		String path = pkg.replace('.', '/');
		Set<String> classes = new HashSet<>();
		try {
			Enumeration<URL> resources = loader.getResources(path);
			while (resources.hasMoreElements()) {
				URL res = resources.nextElement();
				classes.addAll(scanResourceForClasses(res, path));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		String[] array = new String[classes.size()];
		classes.toArray(array);
		Arrays.sort(array);
		return array;
	}

	private static Set<String> scanResourceForClasses(URL url, String path) throws IOException {
		Set<String> result = new HashSet<>();
		URLConnection conn = url.openConnection();

		if (conn instanceof JarURLConnection) {
			JarURLConnection jarConn = (JarURLConnection) conn;
			// Inside a JAR
			JarFile jar = jarConn.getJarFile();
			Enumeration<JarEntry> e = jar.entries();
			while (e.hasMoreElements()) {
				JarEntry entry = e.nextElement();
				String name = entry.getName();
				if (name.endsWith(".class") && name.startsWith(path)) {
					result.add(name.replace('/', '.').substring(0, name.length() - 6));
				}
			}
		} else {
			// Directory on disk
			File dir = new File(url.getPath());
			if (dir.isDirectory()) {
				Files.walk(dir.toPath())
					.filter(p -> p.toString().endsWith(".class"))
					.forEach(p -> {
						String fullPath = p.toString().replace(File.separatorChar, '/');
						int idx = fullPath.indexOf(path);
						if (idx != -1) {
							String className = fullPath.substring(idx)
								.replace('/', '.')
								.replaceAll("\\.class$", "");
							result.add(className);
						}
					});
			}
		}
		return result;
	}

	private static String mapTeaVmToRuntimeClassName(String teavmName) {
		if (!teavmName.startsWith(AWTEA_PREFIX)) return null;

		String withoutPrefix = teavmName.substring(AWTEA_PREFIX.length());
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
